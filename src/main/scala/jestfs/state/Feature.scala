package jestfs.state

import jestfs.cfg.*
import jestfs.es.*
import jestfs.ir.Name
import jestfs.spec.*
import jestfs.ty.AstSingleTy
import jestfs.util.UId

/** ECMAScript features */
sealed trait Feature extends StateElem with UId {
  def func: Func
  def head: Head
  def id: Int = func.id
}
case class SyntacticFeature(
  func: Func,
  head: SyntaxDirectedOperationHead,
) extends Feature
case class BuiltinFeature(
  func: Func,
  head: BuiltinHead,
) extends Feature
