package jestfs.es.util

import jestfs.LINE_SEP
import jestfs.spec.*
import jestfs.util.*
import jestfs.util.Appender.*
import jestfs.util.BaseUtils.*
import jestfs.es.*
import jestfs.es.util.injector.*
import jestfs.es.util.injector.Injector.*
import jestfs.state.*

/** stringifier for ECMAScript */
class Stringifier(
  detail: Boolean,
  location: Boolean,
  grammar: Option[Grammar],
  text: Option[String],
) {
  // elements
  given elemRule: Rule[ESElem] = (app, elem) =>
    elem match
      case elem: Script      => scriptRule(app, elem)
      case elem: Ast         => astRule(app, elem)
      case elem: ConformTest => testRule(app, elem)
      case elem: Assertion   => assertRule(app, elem)

  /** ECMAScript script program */
  given scriptRule: Rule[Script] = (app, script) => app >> script.code

  // abstract syntax tree (AST) values
  given astRule: Rule[Ast] = (app, ast) =>
    grammar match
      case Some(grammar) => grammarAstRule(app, (grammar, ast))
      case None          => basicAstRule(app, ast)

  // span information
  given locRule: Rule[Loc] = (app, loc) => app >> loc.toString

  lazy val grammarAstRule: Rule[(Grammar, Ast)] = (app, pair) =>
    val (grammar, origAst) = pair
    val nameMap = grammar.nameMap

    def aux(ast: Ast): Unit = ast match
      case Lexical(name, str) => app >> str >> " "
      case Syntactic(name, args, rhsIdx, children) =>
        var cs = children
        for (symbol <- nameMap(name).rhsVec(rhsIdx).symbols) symbol match
          case Terminal(term)                          => app >> term >> " "
          case Empty | NoLineTerminator | _: Lookahead =>
          case _ =>
            cs.headOption match
              case Some(hd) => hd.map(aux); cs = cs.tail
              case _        => error(s"invalid AST: $origAst")
    (text, origAst.loc) match
      case (Some(text), Some(loc)) => app >> loc.getString(text)
      case _                       => aux(origAst)
    app

  lazy val basicAstRule: Rule[Ast] = (app, ast) =>
    ast match
      case Syntactic(name, args, rhsIdx, children) =>
        given Rule[Boolean] = (app, bool) => app >> (if (bool) "T" else "F")

        given Rule[List[Boolean]] = iterableRule()

        app >> "|" >> name >> "|"
        if (args.nonEmpty) app >> "[" >> args >> "]"
        app >> "<" >> rhsIdx >> ">"
        if (detail && ast.loc.isDefined) app >> ast.loc.get

        given Rule[Option[Ast]] = optionRule("<none>")

        if (detail) app.wrap("(", ")")(children.map(app :> _ >> ",")) else app
      case Lexical(name, str) =>
        app >> "|" >> name >> "|(" >> str >> ")"
        if (detail && ast.loc.isDefined) app >> ast.loc.get else app

  // conformance tests
  given testRule: Rule[ConformTest] = (app, test) =>
    val ConformTest(id, script, exitTag, defs, isAsync, assertions) = test
    val delayHead = "$delay(() => {"
    val delayTail = "});"

    app >> "// [EXIT] " >> exitTag.toString
    app :> script
    exitTag match {
      case NormalTag =>
        if (defs) {
          app :> "(() => {"
          app :> assertionLib
          if (isAsync) app :> delayHead
          assertions.foreach(app :> _)
          if (isAsync) app :> delayTail
          app :> "})();"
        } else {
          if (isAsync) app :> delayHead
          assertions.foreach(app :> _)
          if (isAsync) app :> delayTail
        }
      case _ =>
    }
    app

  // assertions
  given assertRule: Rule[Assertion] = (app, assert) =>
    given Rule[SimpleValue] = (app, value) =>
      app >> (
        value match
          case Number(n) => n.toString
          case v         => v.toString
      )

    given Rule[Map[String, SimpleValue]] = (app, desc) =>
      app.wrap(
        desc.map((field, value) => app :> field >> ": " >> value >> ","),
      )

    assert match
      case HasValue(x, v) =>
        app >> s"$$assert.sameValue($x, " >> v >> ");"
      case IsExtensible(addr, path, b) =>
        app >> s"$$assert.sameValue(Object.isExtensible($path), $b);"
      case IsCallable(addr, path, b) =>
        app >> s"$$assert.${if b then "c" else "notC"}allable($path);"
      case IsConstructable(addr, path, b) =>
        app >> s"$$assert.${if b then "c" else "notC"}onstructable($path);"
      case CompareArray(addr, path, array) =>
        app >> s"$$assert.compareArray($$Reflect_ownKeys($path), ${array
          .mkString("[", ", ", "]")}, $path);"
      case SameObject(addr, path, origPath) =>
        app >> s"$$assert.sameValue($path, $origPath);"
      case VerifyProperty(addr, path, propStr, desc) =>
        app >> s"$$verifyProperty($path, $propStr, " >> desc >> ");"
}
