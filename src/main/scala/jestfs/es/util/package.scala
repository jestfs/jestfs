package jestfs.es.util

import jestfs.*
import jestfs.es.*
import jestfs.cfg.CFG
import jestfs.util.BaseUtils.warn

/** merge statements to script */
// TODO refactoring
def mergeStmt(l: List[Ast]): Ast =
  val params = List(false, false, false)
  val bodyOpt = l match
    case a :: rest =>
      val init: Ast = Syntactic("StatementList", params, 0, Vector(Some(a)))
      val list = rest.foldLeft(init) {
        case (x, y) =>
          Syntactic("StatementList", Nil, 1, Vector(Some(x), Some(y)))
      }
      Some(Syntactic("ScriptBody", Nil, 0, Vector(Some(list))))
    case Nil => None
  Syntactic("Script", Nil, 0, Vector(bodyOpt))

/** flatten statements */
// TODO refactoring
def flattenStmtList(
  s: Ast,
  list: List[Ast] = Nil,
): List[Ast] = s match
  case Syntactic("StatementList", _, 0, Vector(Some(x0))) =>
    x0 :: list
  case Syntactic("StatementList", _, 1, Vector(Some(x0), Some(x1))) =>
    flattenStmtList(x0, x1 :: list)
  case _ => Nil
def flattenStmt(s: Ast): List[Ast] = s match
  case Syntactic("Script", _, 0, Vector(Some(body))) =>
    body match
      case Syntactic("ScriptBody", _, 0, Vector(Some(stmtList))) =>
        flattenStmtList(stmtList)
      case _ => Nil
  case _ => Nil

/** ECMAScript strict mode directive */
val USE_STRICT = s"\"use strict\";$LINE_SEP"

// -----------------------------------------------------------------------------
// global mutable options and structures
// -----------------------------------------------------------------------------
/** get control flow graph */
def cfg: CFG = globalCFG.getOrElse({
  warn("CFG is not yet initialized. Using default cfg instead.")
  CFG.defaultCFG
})

private var globalCFG: Option[CFG] = None
def withCFG[T](cfg: CFG)(t: => T): T =
  globalCFG = Some(cfg)
  val res = t
  globalCFG = None
  res
