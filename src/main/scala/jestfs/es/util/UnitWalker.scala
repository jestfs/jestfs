package jestfs.es.util

import jestfs.util.BasicUnitWalker
import jestfs.es.*
import jestfs.es.util.injector.*

/** a unit walker for ECMAScript */
trait UnitWalker extends BasicUnitWalker {
  def walk(elem: ESElem): Unit = elem match
    case elem: Script      => walk(elem)
    case elem: Ast         => walk(elem)
    case elem: ConformTest => walk(elem)
    case elem: Assertion   => walk(elem)

  /** ECMAScript script program */
  def walk(script: Script): Unit = {}

  /** ASTs */
  def walk(ast: Ast): Unit = ast match
    case ast: Syntactic => walk(ast)
    case ast: Lexical   => walk(ast)

  /** syntactic productions */
  def walk(ast: Syntactic): Unit = walkVector(ast.children, walkOpt(_, walk))

  /** lexical productions */
  def walk(ast: Lexical): Unit = {}

  /** conformance test */
  def walk(test: ConformTest): Unit = walkVector(test.assertions, walk)

  /** assertions */
  def walk(assert: Assertion): Unit = {}
}
