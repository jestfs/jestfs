package jestfs.es.util.synthesizer

import jestfs.es.*
import jestfs.es.util.*
import jestfs.spec.*
import jestfs.spec.util.GrammarGraph
import jestfs.spec.util.GrammarGraph.*
import jestfs.util.BaseUtils.*
import cats.instances.boolean

/** A random ECMAScript AST synthesizer */
object RandomSynthesizer extends RandomSynthesizer
trait RandomSynthesizer extends Synthesizer {
  import graph.*

  val USE_SIMPLE_PROB = 0.8

  /** synthesizer name */
  def name: String = "RandomSynthesizer"

  /** get script */
  def script: String =
    apply("StatementListItem", List(false, false, false)).toString(grammar)

  /** get initial pool */
  def initPool: Vector[String] = (for {
    _ <- Range(0, INIT_SIZE)
  } yield script).toVector

  private val INIT_SIZE = 1000

  /** for syntactic production */
  def apply(name: String, args: List[Boolean]): Syntactic =
    val synNode = getSyn(name, args)
    val arr = synEdges(synNode).toArray.map(r => r -> weight(r))
    if (arr.toList.exists { case (_, k) => k == 0 })
      println((synNode, arr.toList))
    val rhsNode = weightedChoose(arr)
    val rhs = rhsNode.rhs
    val isSingle = rhs.symbols.length == 1
    val children = rhs.nts.map(synSymbol(_, rhsNode.argMap, !isSingle)).toVector
    Syntactic(name, args, rhsNode.idx, children)

  /** for lexical production */
  def apply(name: String): Lexical = SimpleSynthesizer(name)

  // ---------------------------------------------------------------------------
  // private helpers
  // ---------------------------------------------------------------------------
  protected def chooseRhs(
    prod: Production,
    pairs: Iterable[(Rhs, Int)],
  ): (Rhs, Int) = choose(pairs)

  // cache for shortest AST for each grammar node
  lazy val weight: Map[Node, Int] =
    fixpoint(Map(), topological, aux)(using Ordering[Int].reverse)

  private def aux(
    map: Map[Node, Int],
    node: Node,
  ): Int = node match
    case synNode: SynNode =>
      (for {
        rhsNode <- synEdges.getOrElse(synNode, Set())
      } yield map.getOrElse(rhsNode, 0)).sum
    case lexNode: LexNode => 1
    case rhsNode: RhsNode =>
      rhsNode.rhs.symbols match
        case List(nt: Nonterminal) =>
          map.getOrElse(getProd(nt, rhsNode.argMap), 0)
        case _ => 1

  private def synSymbol(
    nt: Nonterminal,
    argMap: Map[String, Boolean],
    mayUseSimple: Boolean,
  ): Option[Ast] =
    val Nonterminal(name, args, optional) = nt
    getProd(nt, argMap) match
      case lexNode: LexNode =>
        Some(Lexical(name, SimpleSynthesizer.reservedLexicals(name)))
      case synNode: SynNode =>
        if (optional && randDouble < USE_SIMPLE_PROB) None
        else
          val newArgs = synNode.args
          val syn =
            if (mayUseSimple && randDouble < USE_SIMPLE_PROB)
              SimpleSynthesizer(name, newArgs)
            else apply(name, newArgs)
          Some(syn)
}
