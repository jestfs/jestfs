package jestfs.phase

import jestfs.*
import jestfs.error.*
import jestfs.es.Script
import jestfs.es.util.injector.ConformTest.*
import jestfs.es.util.injector.*
import jestfs.js.*
import jestfs.util.*
import jestfs.util.SystemUtils.*
import jestfs.util.BaseUtils.*
import scala.collection.parallel.CollectionConverters._

/** `conform-test` phase */
case object ConformTest
  extends Phase[
    (
      Map[Target, Iterable[Script]],
      Map[Target, Iterable[Script]],
      Iterable[Script],
    ),
    Map[Target, Iterable[String]],
  ] {
  val name = "conform-test"
  val help = "performs conformance test for an JavaScript engine or a transpiler"

  private var _config: Config = null
  private type Input = Map[Target, Iterable[Script]]
  private type Result = Map[Target, Iterable[String]]

  def apply(
    input: (Input, Input, Iterable[Script]),
    cmdConfig: CommandConfig,
    config: Config,
  ): Result =
    _config = config
    config.msgdir.foreach(cleanDir)
    cleanDir(s"$CONFORMTEST_LOG_DIR/msg")

    val (etestMap, ttestMap, originals) = input

    // Generate name->original map
    originals.foreach(s => originalMap += (s.name -> s.code))

    // do test for engines
    val engineResult = etestMap.map((e, tests) =>
      e -> {
        val fails = tests.par
          .flatMap(test => doConformTest(e, false, test))
          .toList

        bugStat += (e -> toBugStat(fails))

        fails.map(_._1)
      },
    )

    // do test for transpilers
    val transResult = ttestMap.map((t, tests) =>
      t -> {
        val fails = tests.par
          .flatMap(test => doConformTest(t, true, test))
          .toList

        bugStat += (t -> toBugStat(fails))

        fails.map(_._1)
      },
    )

    val result: Result = engineResult ++ transResult

    // dump result as JSON and tsv
    dumpJson(result.map(_.toString -> _), s"$CONFORMTEST_LOG_DIR/fails.json")
    dumpJson(
      bugStat.map(_.toString -> _),
      s"$CONFORMTEST_LOG_DIR/bug-stat.json",
    )
    dumpSummary(result, bugStat)

    // return result
    result

  // ---------------------------------------------------------------------------
  // private helpers
  // ---------------------------------------------------------------------------

  private def debug(a: Any): Unit =
    if (_config.debug) println(a)

  // get the original code by name
  private var originalMap: Map[String, String] = Map()

  // bug stat
  private var bugStat: Map[Target, Map[String, Int]] = Map()

  // do conform test, and returns Some((name, bug)) if fails.
  private def doConformTest(
    target: Target,
    isTrans: Boolean,
    script: Script,
  ): Option[(String, String)] =
    val Script(code, name) = script
    debug(s"Testing $target: $name...")

    // Obtain expected result
    val exitTagRaw = exitTagPattern.findFirstIn(code).get
    val exitTag = ExitTag(exitTagRaw).get
    val isNormal = exitTag == NormalTag

    // Obtain concrete result
    val (concreteExitTag, stdout) =
      if (!isTrans) {
        JSEngine
          .runUsingBinary(target.cmd, code)
          .map((NormalTag, _))
          .recover(engineErrorResolver _)
          .get
      } else {
        JSEngine
          .run(code)
          .map((NormalTag, _))
          .recover(engineErrorResolver _)
          .get
      }

    val sameExitTag = exitTag.equivalent(concreteExitTag)
    val pass = sameExitTag && stdout.isEmpty

    // log information when test fails
    if pass then None
    else {
      val detail =
        if (!sameExitTag) then
          s"[Exit Tag Mismatch]$LINE_SEP > Expected $exitTagRaw but got $concreteExitTag"
        else s"[Assertion Fail]$LINE_SEP > $stdout"

      val db = s"$RESOURCE_DIR/bugs/${target.name}"
      val original = originalMap(name)

      val tag = tagFinder(target, db, original, code, detail)

      val msg = s"""Test fail for `$target`: $name
                   |TAG: $tag
                   |$detail
                   |""".stripMargin

      debug(s"$original$LINE_SEP$msg")

      def dumpMsg(dir: String) = {
        val path = s"$dir/$name.msg"
        val orig = optional(readFile(path)).getOrElse(original)
        dumpFile(orig + LINE_SEP + msg, path)
      }
      _config.msgdir.foreach(dumpMsg)
      dumpMsg(s"$CONFORMTEST_LOG_DIR/msg")

      if (_config.saveBugs)
        val shortMsg = s"""TAG: ${tag.toString.replace("TODO", "NEW")}
                          |$detail""".stripMargin
        val data = s"$original$LINE_SEP/* $shortMsg */$LINE_SEP"
        tag match {
          case NewBug(_)        => appendFile(data, s"$db/TODO")
          case TodoBug(name, _) => dumpFile(data, s"$db/TODO/$name")
          case _                =>
        }

      Some((name, tag._tag))
    }

  private val exitTagPattern = "(?<=\\[EXIT\\] )[^\n\r]*".r
  private val errorPattern = "[\\w]*Error(?=: )".r

  private def engineErrorResolver(engineError: Throwable): (ExitTag, String) =
    engineError match {
      case e: TimeoutException     => (TimeoutTag, "")
      case e @ NoCommandError(cmd) => throw e
      case e =>
        val msg = e.getMessage
        val tag = errorPattern.findFirstIn(msg) match {
          case Some(name) =>
            ThrowErrorTag(name, msg.split(LINE_SEP).toList.headOption)
          case _ if msg.contains("TRANSPILE_FAILURE") => TranspileFailTag
          case _ => ThrowValueTag(jestfs.state.Str(msg))
        }
        (tag, "")
    }

  trait Bug(val _tag: String)
  case class KnownBug(val tag: String) extends Bug(tag) {
    override def toString = s"KNOWN-$tag"
  }
  case class TodoBug(val name: String, val tag: String) extends Bug(tag) {
    override def toString = s"TODO-$tag"
  }
  case class NewBug(val tag: String) extends Bug(tag) {
    override def toString = s"NEW-$tag"
  }

  private def tagFinder(
    target: Target,
    db: String,
    original: String,
    code: String,
    detail: String,
  ): Bug =
    // search if this bug is already known
    val buggies = listFiles(db).filter(jsFilter)
    val knownTag = buggies.foldLeft[Bug](NewBug("YET"))((cur, buggy) =>
      if (cur != NewBug("YET")) cur
      else if containsScript(readFile(buggy.getPath), original) then
        KnownBug(buggy.getName.dropRight(3))
      else cur,
    )

    // search if this bug is already in TODO
    val todos = listFiles(s"$db/TODO").filter(jsFilter)
    val todoTag = todos.foldLeft(knownTag)((cur, todo) =>
      if (cur != NewBug("YET")) cur
      else if sameScript(readFile(todo.getPath), original) then
        TodoBug(todo.getName, "YET")
      else cur,
    )

    // apply heuristic to guess tag of this bug
    def matched(rule: Array[String]): Option[String] =
      val Array(tag, codePattern, msgPattern) = rule
      if (
        (
          code.contains(codePattern) ||
          codePattern.endsWith("$") && code.endsWith(codePattern.dropRight(1))
        ) && detail.contains(msgPattern)
      )
        Some(tag)
      else
        None
    val tag = manualRule.foldLeft(todoTag)((cur, rule) =>
      cur match {
        case NewBug("YET") =>
          matched(rule).map(tag => NewBug(tag)).getOrElse(cur)
        case TodoBug(file, "YET") =>
          matched(rule).map(tag => TodoBug(file, tag)).getOrElse(cur)
        case _ => cur
      },
    )

    tag

  private def containsScript(lines: String, script: String): Boolean =
    val scriptLine = script.split(LINE_SEP)(1)
    lines.split(LINE_SEP).exists(_.trim == scriptLine.trim)

  private def sameScript(script1: String, script2: String): Boolean =
    (script1.split(LINE_SEP) zip script2.split(LINE_SEP)).forall(_ == _)

  private def appendFile(data: Any, dir: String) = synchronized {
    val names = listFiles(dir).map(_.getName.dropRight(3).toInt).sorted
    val emptyIdx =
      names.zipWithIndex.find(p => p._1 != p._2).map(_._2).getOrElse(names.size)
    dumpFile(data, s"$dir/$emptyIdx.js")
  }

  private def toBugStat(fails: List[(String, String)]): Map[String, Int] =
    fails
      .map(_._2)
      .foldLeft(Map[String, Int]())((stat, tag) =>
        val origCount = stat.getOrElse(tag, 0)
        stat + (tag -> (origCount + 1)),
      )

  private def dumpSummary(
    result: Result,
    bugStat: Map[Target, Map[String, Int]],
  ) =
    val header = Vector("target", "bug-list", "fail-num")
    val body: List[Vector[String]] = result
      .map((target, fails) =>
        val bugs = bugStat(target).keys.map(_ + "|").mkString("")
        Vector(target.name, bugs, fails.size.toString),
      )
      .toList
    dumpRows(header :: body, s"$CONFORMTEST_LOG_DIR/test-summary.tsv")

  def defaultConfig: Config = Config()
  val options: List[PhaseOption[Config]] = List(
    (
      "debug",
      BoolOption(c => c.debug = true),
      "turn on debug mode",
    ),
    (
      "msgdir",
      StrOption((c, s) => c.msgdir = Some(s)),
      "directory to log msg",
    ),
    (
      "save-bugs",
      BoolOption(c => c.saveBugs = true),
      "save found bugs to database",
    ),
  )
  case class Config(
    var debug: Boolean = false,
    var msgdir: Option[String] = None,
    var saveBugs: Boolean = false,
  )
}
