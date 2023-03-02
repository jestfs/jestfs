package jestfs.state

import jestfs.cfg.*
import jestfs.ir.{Func => IRFunc, *}

/** IR calling contexts */
case class CallContext(retId: Id, context: Context) extends StateElem {

  /** function name * */
  def name: String = context.func.irFunc.name

  /** copy contexts */
  def copied: CallContext = copy(context = context.copied)
}
