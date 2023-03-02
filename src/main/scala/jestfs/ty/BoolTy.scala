package jestfs.ty

import jestfs.util.*
import jestfs.ty.util.Parser

/** boolean types */
case class BoolTy(set: Set[Boolean] = Set())
  extends TyElem
  with Lattice[BoolTy] {
  import BoolTy.*

  /** top check */
  def isTop: Boolean = this == Top

  /** bottom check */
  def isBottom: Boolean = this == Bot

  /** partial order/subset operator */
  def <=(that: => BoolTy): Boolean = this.set subsetOf that.set

  /** union type */
  def ||(that: => BoolTy): BoolTy = BoolTy(this.set ++ that.set)

  /** intersection type */
  def &&(that: => BoolTy): BoolTy = BoolTy(this.set intersect that.set)

  /** prune type */
  def --(that: => BoolTy): BoolTy = BoolTy(this.set -- that.set)
}
object BoolTy extends Parser.From(Parser.boolTy) {
  val Top: BoolTy = BoolTy(Set(false, true))
  val Bot: BoolTy = BoolTy()
}
