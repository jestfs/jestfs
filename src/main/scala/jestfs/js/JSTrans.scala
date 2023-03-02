package jestfs.js

import jestfs.*
import jestfs.error.*
import jestfs.util.SystemUtils.readFile
import scala.util.Try
import sys.process._
import java.util.StringJoiner

/** JavaScript Transpiler utilities */
object JSTrans {

  val defaultCmd = Map(
    "babel" -> s"$RESOURCE_DIR/trans/babel-d.js",
    "swc" -> s"$RESOURCE_DIR/trans/swc-d.js",
    "terser" -> s"$RESOURCE_DIR/trans/terser-d.js",
    "obfuscator" -> s"$RESOURCE_DIR/trans/obfuscator-d.js",
  )

  /** inner minified babel */
  val BABEL_FILE = s"$RESOURCE_DIR/trans/babel@7.19.1.min.js"
  val RUNNER_FILE = s"$RESOURCE_DIR/trans/transpile.js"
  lazy val babel = readFile(BABEL_FILE)
  lazy val runner = readFile(RUNNER_FILE)

  lazy val doInitBabel: Unit =
    JSEngine.runInContext("babel", babel)

  def transpileUsingBabel(src: String): String =
    doInitBabel
    val escaped =
      src.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
    JSEngine.runInContext("babel", s"orig = `$escaped`").get
    JSEngine
      .runInContext("babel", runner)
      .map(_.toString)
      .getOrElse(
        s"throw \"$failTag\";",
      )

  /** transpile file using given binary */
  def transpileFileUsingBinary(runner: String, file: String): Try[String] =
    Try {
      val stdout = new StringJoiner(LINE_SEP)
      val stderr = new StringJoiner(LINE_SEP)
      s"timeout 3s $runner $file" ! ProcessLogger(
        out => stdout.add(out),
        err => stderr.add(err),
      ) match {
        case 0   => stdout.toString
        case 127 => throw NoCommandError(runner)
        case st  => throw TranspileFailureError
      }
    }

  /** transpile directory using given binary */
  def transpileDirUsingBinary(
    runner: String,
    inputDir: String,
    outputDir: String,
  ): Try[Unit] =
    Try {
      Try(s"$runner $inputDir $outputDir" ! ProcessLogger(_ => ()))
        .getOrElse(127) match {
        case 0   =>
        case 127 => throw NoCommandError(runner)
        case st  => throw TranspileFailureError
      }
    }

  /** indicating the result of transpilation was faillure */
  val failTag = "TRANSPILE_FAILURE"

}
