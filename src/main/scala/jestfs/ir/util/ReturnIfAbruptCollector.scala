package jestfs.ir.util

import jestfs.ir.*

/** return-if-abrupt expression collector */
object ReturnIfAbruptCollector:
  def apply(elem: IRElem): List[EReturnIfAbrupt] =
    val collector = new ReturnIfAbruptCollector
    collector.walk(elem)
    collector.exprs.toList
private class ReturnIfAbruptCollector extends UnitWalker:
  var exprs: Vector[EReturnIfAbrupt] = Vector()
  override def walk(expr: Expr): Unit =
    expr match
      case expr: EReturnIfAbrupt => exprs :+= expr
      case _                     =>
    super.walk(expr)
