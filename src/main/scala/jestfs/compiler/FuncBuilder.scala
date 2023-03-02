package jestfs.compiler

import jestfs.ir.{Type => IRType, Param => IRParam, *}
import jestfs.lang.*
import jestfs.spec.*
import scala.collection.mutable.{ListBuffer, Stack}

/** function builder */
case class FuncBuilder(
  spec: Spec,
  kind: FuncKind,
  name: String,
  params: List[IRParam],
  retTy: IRType,
  algo: Algorithm,
  returnContext: Option[Ref] = None,
) {

  /** get an IR function as the result of compilation of an algorithm */
  def getFunc(body: => Inst): Func = Func(
    name == "RunJobs",
    kind,
    name,
    params,
    retTy,
    body,
    Some(algo),
  )

  /** bindings for nonterminals */
  var ntBindings: List[(String, Expr, Option[Int])] = algo.head match
    case SyntaxDirectedOperationHead(Some(target), _, _, _, _) =>
      val rhs = grammar.nameMap(target.lhsName).rhsVec(target.idx)
      val rhsNames = rhs.nts.map(_.name)
      val rhsBindings = rhsNames.zipWithIndex.map {
        case (name, idx) => (name, ENAME_THIS, Some(idx))
      }
      if (rhsNames contains target.lhsName) rhsBindings
      else (target.lhsName, ENAME_THIS, None) :: rhsBindings
    case _ => List()

  /** create a new scope with a given procedure */
  def newScope(f: => Unit): Inst =
    scopes.push(ListBuffer()); f; ISeq(scopes.pop.toList)

  /** set backward egde from ir to lang */
  def withLang(lang: Syntax)(f: => Unit): Unit =
    langs.push(lang); val result = f; langs.pop
    result
  def withLang[T <: IRElem](lang: Syntax)(f: => T): T =
    langs.push(lang); val result = backEdgeWalker(f); langs.pop
    result

  /** add instructions to the current scope */
  def addInst(insts: Inst*): Unit = scopes.head ++= insts
    .flatMap {
      case ISeq(is) => is
      case i        => List(i)
    }
    .map(backEdgeWalker.apply)

  /** add return to resume instruction */
  def addReturnToResume(context: Ref, value: Expr): Unit =
    addInst(
      ICall(newTId, EPop(toStrERef(context, "ReturnCont"), true), List(value)),
    )

  /** get next temporal identifier */
  def newTId: Temp = Temp(nextTId)

  /** get next temporal identifier with expressions */
  def newTIdWithExpr: (Temp, Expr) = { val x = newTId; (x, ERef(x)) }

  /** get closure name */
  def nextCloName: String = s"$name:clo${nextCId}"

  /** get continuation name */
  def nextContName: String = s"$name:cont${nextCId}"

  /** grammar */
  private def grammar: Grammar = spec.grammar

  /** scope stacks */
  private var scopes: Stack[ListBuffer[Inst]] = Stack()

  /** lang stacks */
  val langs: Stack[Syntax] = Stack()

  lazy val backEdgeWalker: BackEdgeWalker = BackEdgeWalker(this)

  // ---------------------------------------------------------------------------
  // Private Helpers
  // ---------------------------------------------------------------------------
  // temporal identifier id counter
  private def nextTId: Int = { val tid = tidCount; tidCount += 1; tid }
  private var tidCount: Int = 0

  // closure id counter
  private def nextCId: Int = { val cid = cidCount; cidCount += 1; cid }
  private var cidCount: Int = 0
}
