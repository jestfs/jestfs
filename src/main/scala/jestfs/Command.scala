package jestfs

import jestfs.phase.*
import jestfs.util.ArgParser

/** commands
  *
  * @tparam Result
  *   the result typeof command
  */
sealed abstract class Command[Result](
  /** command name */
  val name: String,

  /** phase list */
  val pList: PhaseList[Result],
) {
  override def toString: String = pList.toString

  /** help message */
  def help: String

  /** help message */
  def examples: List[String]

  /** show the final result */
  def showResult(res: Result): Unit = println(res)

  /** target name */
  def targetName: String = ""

  /** need target */
  def needTarget: Boolean = targetName != ""

  /** run command with command-line arguments */
  def apply(args: List[String]): Result =
    val cmdConfig = CommandConfig(this)
    val parser = ArgParser(this, cmdConfig)
    val runner = pList.getRunner(parser)
    parser(args)
    JestFs(this, runner(_), cmdConfig)

  /** a list of phases without specific IO types */
  def phases: Vector[Phase[_, _]] = pList.phases

  /** append a phase to create a new phase list */
  def >>[R](phase: Phase[Result, R]): PhaseList[R] = pList >> phase
}

/** base command */
case object CmdBase extends Command("", PhaseNil) {
  val help = "does nothing."
  val examples = Nil
}

/** `help` command */
case object CmdHelp extends Command("help", CmdBase >> Help) {
  val help = "shows help messages."
  val examples = List(
    "jestfs help                  // show help message.",
    "jestfs help extract          // show help message of `extract` command.",
  )
  override val targetName = "[<command>]"
  override val needTarget = false
}

// -----------------------------------------------------------------------------
// Mechanized Specification Extraction
// -----------------------------------------------------------------------------
/** `extract` command */
case object CmdExtract extends Command("extract", CmdBase >> Extract) {
  val help = "extracts specification model from ECMA-262 (spec.html)."
  val examples = List(
    "jestfs extract                           // extract current version.",
    "jestfs extract -extract:target=es2022    // extract es2022 version.",
    "jestfs extract -extract:target=868fe7a   // extract 868fe7a hash version.",
  )
}

/** `compile` command */
case object CmdCompile extends Command("compile", CmdExtract >> Compile) {
  val help = "compiles a specification to an IR program."
  val examples = List(
    "jestfs compile                        # compile spec to IR program.",
    "jestfs compile -extract:target=es2022 # compile es2022 spec to IR program",
  )
}

/** `build-cfg` command */
case object CmdBuildCFG extends Command("build-cfg", CmdCompile >> BuildCFG) {
  val help = "builds a control-flow graph (CFG) from an IR program."
  val examples = List(
    "jestfs build-cfg                          # build CFG for spec.",
    "jestfs build-cfg -extract:target=es2022   # build CFG for es2022 spec.",
  )
}

// -----------------------------------------------------------------------------
// Conformance Test Generation
// -----------------------------------------------------------------------------
/** `fuzz` command */
case object CmdFuzz extends Command("fuzz", CmdBuildCFG >> Fuzz) {
  val help = "generates JavaScript programs via fuzzing."
  val examples = List(
    "jestfs fuzz                 # generate JavaScript programs via fuzzing.",
    "jestfs fuzz -fuzz:out=out   # dump the generated programs to `out`",
  )
  override def showResult(cov: es.util.Coverage): Unit =
    println(s"- generated ${cov.size} JavaScript programs.")
    println(cov)
}

/** `gen-test` command */
case object CmdGenTest extends Command("gen-test", CmdBase >> GenTest) {
  val help =
    "generates conform tests for an JavaScript engine or a transpiler based."
  val examples = List(
    "jestfs gen-test codedir assertiondir       # perform conform test using script and test in dir",
    "jestfs gen-test                            # perform conform test using most recent fuzzing result",
    "jestfs gen-test get-test:engines=\"d8,js\" # perform conformtest for d8 and js",
  )
  override def showResult(
    testMapPair: (
      Map[jestfs.js.Target, Iterable[jestfs.es.Script]],
      Map[jestfs.js.Target, Iterable[jestfs.es.Script]],
      Iterable[jestfs.es.Script],
    ),
  ): Unit =
    val (etestMap, ttestMap, _) = testMapPair
    etestMap.foreach {
      case (engine, tests) =>
        println(s"${tests.size} tests generated for the engine `$engine`.")
    }
    ttestMap.foreach {
      case (trans, tests) =>
        println(s"${tests.size} tests generated for the transpiler `$trans`.")
    }
}

/** `comform-test` command */
case object CmdConformTest
  extends Command("conform-test", CmdGenTest >> ConformTest) {
  val help = "performs conform test for an JavaScript engine or a transpiler."
  val examples = List(
    "jestfs comform-test                     # perform conform test",
  )
  override def showResult(
    fails: Map[jestfs.js.Target, Iterable[String]],
  ): Unit =
    fails.foreach {
      case (e, fails) =>
        println(s"failing tests for `$e`: ")
        fails.foreach(f => println(s"  $f"))
    }
}
