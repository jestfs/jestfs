package jestfs.ir.util

import jestfs.ir.*

/** allocation site setters */
object AllocSiteSetter:
  def apply(elem: IRElem): Unit = (new AllocSiteSetter).walk(elem)
private class AllocSiteSetter extends UnitWalker:
  private var asiteCount: Int = 0
  private def newAsite: Int = { val id = asiteCount; asiteCount += 1; id }
  override def walk(expr: AllocExpr): Unit =
    expr.asite = newAsite
    super.walk(expr)
