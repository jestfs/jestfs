package jestfs.util

import jestfs.MANUALS_DIR
import jestfs.spec.Spec
import jestfs.util.BaseUtils.*
import jestfs.util.SystemUtils.*
import java.io.File

/** manual information helpers */
case class ManualInfo(version: Option[Spec.Version]) {
  import ManualInfo.*

  /** get algorithm files */
  def algoFiles: List[File] = getAlgos(paths)

  /** get IR function files */
  def funcFiles: List[File] = getFuncs(paths)

  /** get compile rules */
  def compileRule: CompileRule = getCompileRule(paths)

  /** get compile rules */
  def bugfixFile: Option[File] = bugfixPath.map(File(_))
  def bugfixPath: Option[String] = version.fold(None)(version => {
    val patchFile = s"$MANUALS_DIR/${version.shortHash}/bugfix.patch"
    if (exists(patchFile)) Some(patchFile) else None
  })

  private def getAlgos(paths: List[String]): List[File] =
    getFiles(paths, algoFilter)
  private def getFuncs(paths: List[String]): List[File] =
    getFiles(paths, irFilter)
  private def getFiles(
    paths: List[String],
    filter: String => Boolean,
  ): List[File] = for {
    path <- paths
    file <- walkTree(s"$MANUALS_DIR/$path", sort = true)
    if filter(file.getName)
  } yield file
  private def getCompileRule(paths: List[String]): CompileRule = paths
    .map(path => s"$MANUALS_DIR/$path/rule.json")
    .map(path => optional(readJson[CompileRule](path)).getOrElse(Map()))
    .foldLeft[CompileRule](Map())(_ ++ _)
  private lazy val paths: List[String] =
    List("default") ++ version.map(_.shortHash)
}
object ManualInfo:
  type CompileRule = Map[String, Map[String, String]]
