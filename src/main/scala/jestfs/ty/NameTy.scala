package jestfs.ty

import jestfs.ty.util.Parser
import jestfs.util.*

/** named record types */
case class NameTy(set: BSet[String] = Fin())
  extends TyElem
  with Lattice[NameTy] {
  import NameTy.*
  import TyModel.es.isSubTy

  /** top check */
  def isTop: Boolean = (this eq Top) || (set eq Inf)

  /** bottom check */
  def isBottom: Boolean = (this eq Bot) || (set == Fin())

  /** partial order/subset operator */
  def <=(that: => NameTy): Boolean =
    (this eq that) || ((this.set, that.set) match
      case (_, Inf)               => true
      case (Inf, _)               => false
      case (Fin(lset), Fin(rset)) => isSubTy(lset, rset)
    )

  /** union type */
  def ||(that: => NameTy): NameTy = (this.set, that.set) match
    case _ if this eq that      => this
    case (_, Inf) | (Inf, _)    => Top
    case _ if this.isBottom     => that
    case _ if that.isBottom     => this
    case (Fin(lset), Fin(rset)) => NameTy(Fin(lset ++ rset)).norm

  /** intersection type */
  def &&(that: => NameTy): NameTy = (this.set, that.set) match
    case _ if this eq that => this
    case (_, Inf)          => this
    case (Inf, _)          => that
    case (Fin(lset), Fin(rset)) =>
      val newSet =
        lset.filter(isSubTy(_, rset)) ++ rset.filter(isSubTy(_, lset))
      NameTy(Fin(newSet)).norm

  /** prune type */
  def --(that: => NameTy): NameTy = (this.set, that.set) match
    case (_, Inf)               => Bot
    case (Inf, _)               => Top
    case (Fin(lset), Fin(rset)) => NameTy(Fin(lset.filter(!isSubTy(_, rset))))

  // normalization
  def norm: NameTy = this.set match
    case Inf      => this
    case Fin(set) => NameTy(Fin(set.filter(x => !isSubTy(x, set - x))))
}
object NameTy extends Parser.From(Parser.nameTy) {
  val Top: NameTy = NameTy(Inf)
  val Bot: NameTy = NameTy()
}
