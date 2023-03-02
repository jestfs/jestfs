package jestfs.ir.util

import jestfs.ir.*

/** weak unique id setters */
object WeakUIdSetter:
  def apply(elem: IRElem): Unit = (new WeakUIdSetter).walk(elem)
private class WeakUIdSetter extends UnitWalker:
  var iidCount: Int = 0
  def newIId: Int = { val id = iidCount; iidCount += 1; id }
  var eidCount: Int = 0
  def newEId: Int = { val id = eidCount; eidCount += 1; id }
  override def walk(inst: Inst): Unit = { inst.id = newIId; super.walk(inst) }
  override def walk(expr: Expr): Unit = { expr.id = newEId; super.walk(expr) }
