package jestfs.ir

import jestfs.ir.util.Parser
import jestfs.lang.Syntax
import jestfs.util.WeakUId

// IR instructions
sealed trait Inst extends IRElem with LangEdge with WeakUId[Inst] with NodeEdge:
  // conversion to instruction lists
  def toList: List[Inst] = this match
    case ISeq(is) => is
    case i        => List(i)
object Inst extends Parser.From(Parser.inst)

// normal instructions
sealed trait NormalInst extends Inst
case class IExpr(expr: Expr) extends NormalInst
case class ILet(lhs: Name, expr: Expr) extends NormalInst
case class IAssign(ref: Ref, expr: Expr) extends NormalInst
case class IDelete(ref: Ref) extends NormalInst
case class IPush(from: Expr, to: Expr, front: Boolean) extends NormalInst
case class IRemoveElem(list: Expr, elem: Expr) extends jestfs.ir.NormalInst
case class IReturn(expr: Expr) extends NormalInst with Return
case class IAssert(expr: Expr) extends NormalInst
case class IPrint(expr: Expr) extends NormalInst
case class INop() extends NormalInst
object NormalInsts extends Parser.From(Parser.normalInsts)

// call instructions
sealed trait CallInst extends Inst {
  val lhs: Local
  def fexpr: Expr
}
case class ICall(lhs: Local, fexpr: Expr, args: List[Expr]) extends CallInst
case class IMethodCall(lhs: Local, base: Ref, method: String, args: List[Expr])
  extends CallInst {
  lazy val fexpr: Expr = ERef(Prop(base, EStr(method)))
}
case class ISdoCall(lhs: Local, base: Expr, method: String, args: List[Expr])
  extends CallInst {
  lazy val fexpr: Expr = base match
    case ERef(base) => ERef(Prop(base, EStr(method)))
    case _          => ??? // XXX error -> see compiler
}

// special instructions
case class ISeq(insts: List[Inst]) extends Inst
case class IIf(cond: Expr, thenInst: Inst, elseInst: Inst) extends Inst
case class ILoop(kind: String, cond: Expr, body: Inst) extends Inst
