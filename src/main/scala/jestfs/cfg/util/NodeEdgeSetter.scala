package jestfs.cfg.util

import jestfs.cfg.*
import jestfs.ir.{Func => IRFunc, *}
import jestfs.ir.util.{UnitWalker => IRUnitWalker}

/** node edge setter */
object NodeEdgeSetter:
  def apply(cfg: CFG): Unit = for (func <- cfg.funcs) apply(func)
  def apply(func: Func): Unit = for (node <- func.nodes) apply(node)
  def apply(node: Node): Unit =
    val setter = new NodeEdgeSetter(node)
    node match
      case block: Block   => for (inst <- block.insts) setter.walk(inst)
      case call: Call     => setter.walk(call.callInst)
      case branch: Branch => setter.walk(branch.cond)
private class NodeEdgeSetter(node: Node) extends IRUnitWalker:
  override def walk(inst: Inst): Unit =
    inst.cfgNode = Some(node); super.walk(inst)
  override def walk(expr: Expr): Unit =
    expr.cfgNode = Some(node); super.walk(expr)
