package jestfs.es.util

import jestfs.util.BasicWalker
import jestfs.es.*
import jestfs.es.util.injector.*

/** a walker for ECMAScript */
trait Walker extends BasicWalker {
  def walk(elem: ESElem): ESElem = elem match
    case elem: Script      => walk(elem)
    case elem: Ast         => walk(elem)
    case elem: ConformTest => walk(elem)
    case elem: Assertion   => walk(elem)

  /** ECMAScript script program */
  def walk(script: Script): Script = script

  /** ASTs */
  def walk(ast: Ast): Ast = ast match
    case ast: Syntactic => walk(ast)
    case ast: Lexical   => walk(ast)

  /** syntactic productions */
  def walk(ast: Syntactic): Syntactic =
    val Syntactic(name, args, rhsIdx, children) = ast
    Syntactic(name, args, rhsIdx, walkVector(children, walkOpt(_, walk)))

  /** lexical productions */
  def walk(ast: Lexical): Lexical = ast

  /** conformance test */
  def walk(test: ConformTest): ConformTest =
    val ConformTest(id, script, exitTag, defs, isAsync, assertions) = test
    ConformTest(
      id,
      script,
      exitTag,
      defs,
      isAsync,
      walkVector(assertions, walk),
    )

  /** assertions */
  def walk(assert: Assertion): Assertion = assert
}
