package jestfs.phase

import jestfs.*
import jestfs.es.util.*
import jestfs.util.*
import jestfs.util.SystemUtils.*
import jestfs.util.BaseUtils.*
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax._

/** `categorize-bugk` phase */
case object CategorizeBug extends Phase[Unit, Map[String, Map[String, Int]]] {
  val name = "categorize-bug"
  val help = "categorizes detected bugs"

  type Target = String
  type Tag = String
  type Test = String

  def apply(
    unit: Unit,
    cmdConfig: CommandConfig,
    config: Config,
  ): Map[Target, Map[Tag, Int]] =
    // name of json files
    val scripts = cmdConfig.targets(0)
    val failsMapJson = cmdConfig.targets(1)
    val failsMap: Map[Target, Set[Test]] = readJson(failsMapJson)

    val init: Map[Target, Map[Target, Int]] = Map()
    val result = failsMap.foldLeft(init) {
      case (cur, (target, fails)) =>
        val db = s"$RESOURCE_DIR/bugs/$target"
        cur + (target -> fails.foldLeft(Map[Target, Int]()) {
          case (count, test) =>
            val script = readFile(s"$scripts/$test")
            val tag = tagFinder(db, script)
            if (!blackList.contains(tag))
              val c = count.getOrElse(tag, 0) + 1
              count + (tag -> c)
            else count
        })
    }

    dumpJson(result, s"$CATEGORIZE_LOG_DIR/result.json")
    dumpSummary(result)

    result

  private def tagFinder(db: String, script: String) =
    val buggies = listFiles(db).filter(jsFilter)
    buggies.foldLeft("YET")((cur, buggy) =>
      if (cur != "YET") cur
      else {
        if containsScript(readFile(buggy.getPath), script) then
          buggy.getName.dropRight(3)
        else cur
      },
    )

  private def containsScript(lines: String, script: String): Boolean =
    val scriptLine = script.split(LINE_SEP)(1)
    lines.split(LINE_SEP).exists(_.trim == scriptLine.trim)

  private def dumpSummary(result: Map[Target, Map[Tag, Int]]) =
    val header = Vector("target", "bug-list", "fail-num")
    val body: List[Vector[String]] = result
      .map((target, bugCount) =>
        val bugs = bugCount.keys.map(_ + "|").mkString("")
        val failNum = bugCount.values.foldLeft(0)(_ + _)
        Vector(target, bugs, failNum.toString),
      )
      .toList
    dumpRows(header :: body, s"$CATEGORIZE_LOG_DIR/test-summary.tsv")

  private val blackList = List("OBF-V8", "ECM-01", "YET")

  def defaultConfig: Config = Config()
  val options: List[PhaseOption[Config]] = List()
  case class Config()
}
