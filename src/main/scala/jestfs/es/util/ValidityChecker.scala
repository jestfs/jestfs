package jestfs.es.util

import jestfs.*
import jestfs.es.*
import jestfs.spec.*
import jestfs.js.JSEngine
import scala.util.Try

/** ECMAScript program validity checker */
object ValidityChecker {
  def apply(grammar: Grammar, ast: Ast): Boolean = apply(ast.toString(grammar))
  def apply(code: String): Boolean =
    val src = s"${USE_STRICT}throw \"$MESSAGE\";$LINE_SEP;$LINE_SEP$code"
    (!useD8 || _valid(JSEngine.runUsingD8(src))) &&
    (!useJs || _valid(JSEngine.runUsingJs(src)))

  val MESSAGE = "VALIDITY_CHECKER_EXPECTED_EXCEPTION"

  private def _valid(result: Try[Any]): Boolean =
    result.failed.filter(_.getMessage contains MESSAGE).isSuccess

  import jestfs.util.BaseUtils.warn
  lazy val useD8: Boolean =
    val use = JSEngine.runUsingD8(";").isSuccess;
    if (!use) warn("No D8")
    use
  lazy val useJs: Boolean =
    val use = JSEngine.runUsingJs(";").isSuccess;
    if (!use) warn("No Graal")
    use
}
