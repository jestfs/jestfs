package jestfs.es.util.synthesizer

import jestfs.cfg.*
import jestfs.es.*
import jestfs.es.util.*
import jestfs.spec.Grammar
import jestfs.spec.util.GrammarGraph
import jestfs.spec.util.GrammarGraph.*

/** ECMAScript AST synthesizer */
trait Synthesizer {

  /** synthesizer name */
  def name: String

  /** get script */
  def script: String

  /** get initial pool */
  def initPool: Vector[String]

  /** for general production */
  def apply(ast: Ast): Ast = ast match
    case ast: Syntactic => apply(ast)
    case ast: Lexical   => apply(ast)

  /** for syntactic production */
  def apply(name: String, args: List[Boolean]): Syntactic
  def apply(ast: Syntactic): Syntactic = apply(ast.name, ast.args)

  /** for lexical production */
  def apply(name: String): Lexical
  def apply(ast: Lexical): Lexical = apply(ast.name)

  /** ECMAScript grammar */
  final lazy val grammar: Grammar = cfg.grammar

  // grammar graph
  final lazy val graph = cfg.grammarGraph
}
