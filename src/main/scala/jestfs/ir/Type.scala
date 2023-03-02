package jestfs.ir

import jestfs.ir.util.Parser
import jestfs.lang.{Type => LangType}
import jestfs.ty.*

/** IR types */
case class Type(
  ty: Ty,
  langTy: Option[LangType] = None,
) extends IRElem {

  /** completion check */
  def isDefined: Boolean = ty.isDefined

  /** completion check */
  def isCompletion: Boolean = ty.isCompletion
}
object Type extends Parser.From(Parser.irType)

/** IR unknown types */
val UnknownType: Type = Type(UnknownTy())
def UnknownType(
  msg: String,
  langTy: Option[LangType] = None,
): Type = Type(UnknownTy(Some(msg)), langTy)
