package jestfs.es.util.mutator

import jestfs.es.*
import jestfs.es.util.*
import jestfs.es.util.Coverage.*
import jestfs.spec.Grammar
import jestfs.util.*

/** ECMAScript AST mutator */
trait Mutator {
  private type Result = Seq[(String, Ast)]

  /** mutate string */
  def apply(code: String, n: Int): Result =
    apply(code, n, None)
  def apply(
    code: String,
    n: Int,
    target: Option[(CondView, Coverage)],
  ): Result = apply(cfg.scriptParser.from(code), n, target)

  /** mutate asts */
  def apply(ast: Ast, n: Int): Result = apply(ast, n, None)
  def apply(
    ast: Ast,
    n: Int,
    target: Option[(CondView, Coverage)],
  ): Result

  /** Possible names of underlying mutators */
  val names: List[String]
  lazy val name: String = names.head
}
