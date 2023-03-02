package jestfs.cfg.util

import jestfs.LINE_SEP
import jestfs.cfg.*
import jestfs.ir.{Func => IRFunc, *}
import jestfs.util.*
import jestfs.util.Appender.*
import jestfs.util.BaseUtils.*

/** stringifier for CFG */
class Stringifier(detail: Boolean, location: Boolean) {
  // stringifier for IR
  val irStringifier = IRElem.getStringifier((detail, location))
  import irStringifier.{given, *}

  // elements
  given elemRule: Rule[CFGElem] = (app, elem) =>
    elem match {
      case elem: CFG        => cfgRule(app, elem)
      case elem: Func       => funcRule(app, elem)
      case elem: Node       => nodeRule(app, elem)
      case elem: BranchKind => branchKindRule(app, elem)
    }

  // control-flow graphs (CFGs)
  given cfgRule: Rule[CFG] = (app, cfg) =>
    given Rule[Iterable[Func]] = iterableRule(sep = LINE_SEP)
    given Ordering[Func] = Ordering.by(_.id)
    app >> cfg.funcs.sorted

  // functions
  given funcRule: Rule[Func] = (app, func) =>
    val IRFunc(main, kind, name, params, retTy, _, _) = func.irFunc
    given Rule[Iterable[Param]] = iterableRule("(", ", ", ")")
    app >> func.id >> ": "
    app >> (if (main) "@main " else "") >> "def " >> kind
    app >> name >> params >> ": " >> retTy >> " "
    app.wrap {
      given Ordering[Node] = Ordering.by(_.id)
      for (node <- func.nodes.toList.sorted) app :> node
    }

  // nodes
  given nodeRule: Rule[Node] = (app, node) =>
    app >> node.id >> ": "
    node match
      case Block(_, insts, next) =>
        insts match
          case Vector(inst) => app >> inst
          case _            => app.wrap(for (inst <- insts) app :> inst)
        next.map(x => app >> " -> " >> x.id)
      case other: NodeWithInst => app >> other
    app

  // nodes withs instruction backward edge
  // TODO handle location option
  given nodeWithInstRule: Rule[NodeWithInst] = (app, node) =>
    node match
      case Call(_, callInst, next) =>
        app >> callInst
        next.map(x => app >> " -> " >> x.id)
      case Branch(_, kind, cond, thenNode, elseNode) =>
        app >> kind >> " " >> cond
        thenNode.map(x => app >> " then " >> x.id)
        elseNode.map(x => app >> " else " >> x.id)
    app

  // branch kinds
  given branchKindRule: Rule[BranchKind] = (app, kind) =>
    import BranchKind.*
    app >> (kind match {
      case If        => "if"
      case Loop(str) => s"loop[$str]"
    })
}
