package jestfs.ty

import jestfs.util.*
import jestfs.state.*
import jestfs.ty.util.Parser

/** completion record types */
case class CompTy(
  normal: PureValueTy = PureValueTy.Bot,
  abrupt: BSet[String] = Fin(),
) extends TyElem
  with Lattice[CompTy] {
  import CompTy.*

  /** top check */
  def isTop: Boolean =
    if (this eq Top) true
    else if (this eq Bot) false
    else {
      this.normal.isTop &&
      this.abrupt.isTop
    }

  /** bottom check */
  def isBottom: Boolean =
    if (this eq Bot) true
    else if (this eq Top) false
    else
      (
        this.normal.isBottom &&
        this.abrupt.isBottom
      )

  /** partial order/subset operator */
  def <=(that: => CompTy): Boolean = (this eq that) || (
    this.normal <= that.normal &&
    this.abrupt <= that.abrupt
  )

  /** union type */
  def ||(that: => CompTy): CompTy =
    if (this eq that) this
    else
      CompTy(
        this.normal || that.normal,
        this.abrupt || that.abrupt,
      )

  /** intersection type */
  def &&(that: => CompTy): CompTy =
    if (this eq that) this
    else
      CompTy(
        this.normal && that.normal,
        this.abrupt && that.abrupt,
      )

  /** prune type */
  def --(that: => CompTy): CompTy =
    if (that.isBottom) this
    else
      CompTy(
        this.normal -- that.normal,
        this.abrupt -- that.abrupt,
      )
}
object CompTy extends Parser.From(Parser.compTy) {
  val Bot: CompTy = CompTy()
  val Top = CompTy(PureValueTy.Top, Inf)
}
