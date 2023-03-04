package jestfs.phase

import jestfs.*
import jestfs.cfg.*
import jestfs.es.util.*
import jestfs.es.util.Coverage.*
import jestfs.util.*
import jestfs.util.SystemUtils.*
import jestfs.util.BaseUtils.*
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax._
import scala.collection.mutable.{Map => MMap}
import java.io.PrintWriter

/** `localize` phase */
case object HandleCoverage extends Phase[CFG, Unit] {
  val name = "handle-coverage"
  val help = "handles coverage"

  private var _config: Config = null
  private var jsonProtocol: Option[JsonProtocol] = None

  def apply(cfg: CFG, cmdConfig: CommandConfig, config: Config): Unit =
    withCFG(cfg) {
      _config = config
      jsonProtocol = Some(JsonProtocol(cfg))

      val test262Dir = cmdConfig.targets(0)
      val allDir = cmdConfig.targets(1)
      /*
      // Compare test 262 with fuzzer
      val header = Vector("sens", "left-only", "both", "right-only")
      val body = for {
        k <- Range(0, 3)
        cp <- List(false, true)
        if (k > 0 || !cp)
      } yield compareCoverage(test262Dir, allDir, k, cp)
      dumpRows(header +: body, s"$HANDLE_COVERAGE_LOG_DIR/test262-cmp.csv")
       */
      // draw #call-path histogram
      drawKGraph(1, allDir)
      drawKGraph(2, allDir)
      drawCpGraph(1, allDir)
      drawCpGraph(2, allDir)
      maximumTxt.flush
      maximumTxt.close
    }

  private def getViewsPair(dir: String): (List[NodeView], List[CondView]) =
    val proto = jsonProtocol.get
    import proto.given

    val nodeJson = s"$dir/node-coverage.json"
    val nodeViewInfos: List[NodeViewInfo] = readJson(nodeJson)
    val nodeViews = nodeViewInfos.map(_.nodeView)

    val condJson = s"$dir/branch-coverage.json"
    val condViewInfos: List[CondViewInfo] = readJson(condJson)
    val condViews = condViewInfos.map(_.condView)

    (nodeViews, condViews)

  private def compareCoverage(
    dir1: String,
    dir2: String,
    k: Int,
    cp: Boolean,
  ): Vector[String] =
    val sens = if (cp) then s"$k-cp" else s"$k"

    val (nodeViews1, condViews1) = getViewsPair(dir1)
    val (nodeViews2, condViews2) = getViewsPair(s"$dir2/$sens")

    val nv1 = nodeViews1.map(_.lower(k, cp)).toSet
    val nv2 = nodeViews2.map(_.lower(k, cp)).toSet
    val nv = nv1 & nv2

    val cv1 = condViews1.map(_.lower(k, cp)).toSet
    val cv2 = condViews2.map(_.lower(k, cp)).toSet
    val cv = cv1 & cv2

    val both = nv.size + cv.size
    val leftOnly = nv1.size + cv1.size - both
    val rightOnly = nv2.size + cv2.size - both

    println(s"[$sens]")
    println(s" - both     : $both")
    println(s" - left only  : $leftOnly")
    println(s" - right only : $rightOnly")

    Vector(sens, leftOnly, both, rightOnly).map(_.toString)

  private def countBy[T, G](iter: Iterable[T], group: T => G): Map[G, Int] =
    iter.groupMapReduce(group)(_ => 1)(_ + _)

  private def drawKGraph(k: Int, basedir: String) =
    println(s"Drawing k-graph for ${k - 1} vs $k...")
    maximumTxt.println(s"Drawing k-graph for ${k - 1} vs $k...")

    val dir1 = s"$basedir/${k - 1}"
    val dir2 = s"$basedir/$k"

    val (nodeViews1, condViews1) = getViewsPair(dir1)
    val (nodeViews2, condViews2) = getViewsPair(dir2)

    val nodeCount2 = countBy(nodeViews2, _.lower(k - 1, false))
    val condCount2 = countBy(condViews2, _.lower(k - 1, false))

    val nodeCount1 = nodeViews1.map(nv => (nv, nodeCount2.getOrElse(nv, 0)))
    val condCount1 = condViews1.map(cv => (cv, condCount2.getOrElse(cv, 0)))
    val totalCount = nodeCount1 ++ condCount1

    val encCount = countBy(totalCount, _._2).toSeq.sortBy(_._1)

    val max = encCount.last._1
    println(s"Maximum: $max")
    maximumTxt.println(s"Maximum: $max")
    totalCount
      .filter(_._2 == max)
      .foreach((view, _count) =>
        println(s" - $view")
        maximumTxt.println(s" - $view"),
      )

    val header = Vector("#enclosing", "#test requirement")
    val body =
      Range(0, max + 1).map(k =>
        Vector(k, encCount.toMap.getOrElse(k, 0)).map(_.toString),
      )
    dumpRows(header +: body, s"$HANDLE_COVERAGE_LOG_DIR/$k-graph.tsv")

  private def drawCpGraph(k: Int, basedir: String) =
    println(s"Drawing cp-graph for $k vs $k-cp...")
    maximumTxt.println(s"Drawing cp-graph for $k vs $k-cp...")

    val dir1 = s"$basedir/$k"
    val dir2 = s"$basedir/$k-cp"

    val (nodeViews1, condViews1) = getViewsPair(dir1)
    val (nodeViews, condViews) = getViewsPair(dir2)

    val nodeCount2 = countBy(nodeViews, _.lower(k, false))
    val condCount2 = countBy(condViews, _.lower(k, false))

    val nodeCount1 = nodeViews1.map(nv => (nv, nodeCount2.getOrElse(nv, 0)))
    val condCount1 = condViews1.map(cv => (cv, condCount2.getOrElse(cv, 0)))
    val totalCount = nodeCount1 ++ condCount1

    val cpCount = countBy(totalCount, _._2).toSeq.sortBy(_._1)

    val max = cpCount.last._1
    println(s"Maximum: $max")
    maximumTxt.println(s"Maximum: $max")
    totalCount
      .filter(_._2 == max)
      .foreach((view, _count) =>
        println(s" - $view")
        maximumTxt.println(s" - $view"),
      )

    val header = Vector("#call path", "#test requirement")
    val body = Range(0, max + 1).map(k =>
      Vector(k, cpCount.toMap.getOrElse(k, 0)).map(_.toString),
    )
    dumpRows(header +: body, s"$HANDLE_COVERAGE_LOG_DIR/$k-cp-graph.tsv")

  private lazy val maximumTxt: PrintWriter = getPrintWriter(
    s"$HANDLE_COVERAGE_LOG_DIR/maximum.txt",
  )

  def defaultConfig: Config = Config()
  val options: List[PhaseOption[Config]] = List()
  case class Config()
}
