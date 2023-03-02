package jestfs.es

import jestfs.es.util.*
import jestfs.ir.Type
import jestfs.spec.*
import jestfs.util.*
import scala.annotation.tailrec

/** abstract syntax tree (AST) values */
sealed trait Ast extends ESElem with Locational {

  /** production names */
  val name: String

  /** parent */
  var parent: Option[Ast] = None

  /** idx of production */
  def idx: Int = this match
    case lex: Lexical               => 0
    case Syntactic(_, _, rhsIdx, _) => rhsIdx

  /** validity check */
  def valid(grammar: Grammar): Boolean = ValidityChecker(grammar, this)

  /** size */
  lazy val size: Int = this match
    case lex: Lexical => 1
    case syn: Syntactic =>
      syn.children.map(_.fold(1)(_.size)).foldLeft(1)(_ + _)

  /** production chains */
  lazy val chains: List[Ast] = this match
    case lex: Lexical => List(this)
    case syn: Syntactic =>
      syn.children.flatten match
        case Vector(child) => this :: child.chains
        case _             => List(this)

  /** children */
  def getChildren(kind: String): List[Ast] = this match
    case lex: Lexical => List()
    case Syntactic(k, _, _, children) =>
      val founded = (for {
        child <- children.flatten
        found <- child.getChildren(kind)
      } yield found).toList
      if (k == kind) this :: founded else founded

  /** types */
  lazy val types: Set[String] =
    Set(name, s"$name$idx") union (this match
      case Syntactic(_, _, _, cs) =>
        (cs match
          case Vector(Some(child)) => child.types
          case _                   => Set()
        ) + "Nonterminal"
      case _: Lexical => Set()
    ) + "ParseNode"

  /** flatten statements */
  // TODO refactoring
  def flattenStmt: List[Ast] = this match
    case Syntactic("Script", _, 0, Vector(Some(body))) =>
      body match
        case Syntactic("ScriptBody", _, 0, Vector(Some(stlist))) =>
          flattenStmtList(stlist)
        case _ => Nil
    case _ => Nil

  /** clear location */
  def clearLoc: Ast =
    this match
      case syn: Syntactic =>
        for { child <- syn.children.flatten } child.clearLoc
        syn.loc = None; syn
      case lex: Lexical => lex.loc = None; lex

  /** set location including children */
  def setBaseLoc(loc: Loc): Ast =
    this match
      case syn: Syntactic =>
        for { child <- syn.children.flatten } child.setBaseLoc(loc)
      case _ =>
    this.loc.map(origLoc => this.loc = Some(loc.start + origLoc)); this

  /** not use case class' hash code */
  override def hashCode: Int = super.hashCode
}

/** ASTs constructed by syntactic productions */
case class Syntactic(
  name: String,
  args: List[Boolean],
  rhsIdx: Int,
  children: Vector[Option[Ast]],
) extends Ast {

  /** sub index */
  lazy val subIdx: Int =
    children.map(child => if (child.isDefined) 1 else 0).fold(0)(_ * 2 + _)
}

/** ASTs constructed by lexical productions */
case class Lexical(
  name: String,
  str: String,
) extends Ast
