package jestfs.ty

import jestfs.util.*
import jestfs.state.*
import jestfs.ty.util.Parser

/** AST value types */
sealed trait AstValueTy extends TyElem with Lattice[AstValueTy] {
  import AstValueTy.*

  /** bottom check */
  def isTop: Boolean = this eq Top

  /** bottom check */
  def isBottom: Boolean = this == Bot

  /** partial order/subset operator */
  def <=(that: => AstValueTy): Boolean = (this eq that) || (
    (this, that) match
      case (Bot, _) | (_, AstTopTy)         => true
      case (l: AstSingleTy, r: AstSingleTy) => l == r
      case (l: AstNonTopTy, r: AstNonTopTy) =>
        l.toName.names subsetOf r.toName.names
      case _ => false
  )

  /** union type */
  def ||(that: => AstValueTy): AstValueTy =
    if (this eq that) this
    else
      (this, that) match
        case (AstTopTy, _) | (_, AstTopTy)              => AstTopTy
        case (Bot, _)                                   => that
        case (_, Bot)                                   => this
        case (l: AstSingleTy, r: AstSingleTy) if l == r => l
        case (l: AstNonTopTy, r: AstNonTopTy) =>
          AstNameTy(l.toName.names ++ r.toName.names)

  /** intersection type */
  def &&(that: => AstValueTy): AstValueTy =
    if (this eq that) this
    else
      (this, that) match
        case _ if this <= that => this
        case _ if that <= this => that
        case (AstNameTy(lset), AstNameTy(rset)) =>
          AstNameTy(lset intersect rset)
        case _ => Bot

  /** prune type */
  def --(that: => AstValueTy): AstValueTy =
    if (that.isBottom) this
    else
      (this, that) match
        case _ if this <= that                  => Bot
        case (AstNameTy(lset), AstNameTy(rset)) => AstNameTy(lset -- rset)
        case _                                  => this
}
case object AstTopTy extends AstValueTy
sealed trait AstNonTopTy extends AstValueTy {
  def toName: AstNameTy = this match
    case ty: AstNameTy           => ty
    case AstSingleTy(name, _, _) => AstNameTy(Set(name))
}
case class AstNameTy(names: Set[String] = Set()) extends AstNonTopTy
case class AstSingleTy(name: String, idx: Int, subIdx: Int) extends AstNonTopTy
object AstValueTy extends Parser.From(Parser.astValueTy) {
  val Top = AstTopTy
  val Bot: AstNameTy = AstNameTy()
}
