package jestfs.es.util.mutator

import jestfs.cfg.CFG
import jestfs.es.*
import jestfs.es.util.synthesizer.*
import jestfs.es.util.{Walker => AstWalker, *}
import jestfs.es.util.Coverage.*
import jestfs.spec.Grammar
import jestfs.state.Nearest
import jestfs.util.*
import jestfs.util.BaseUtils.*
import jestfs.ty.AstSingleTy

/** A nearest ECMAScript AST mutator */
class NearestMutator(
  val synthesizer: Synthesizer = RandomSynthesizer,
) extends Mutator {

  val names = "NearestMutator" :: RandomMutator.default.names

  /** mutate programs */
  def apply(
    ast: Ast,
    n: Int,
    target: Option[(CondView, Coverage)],
  ): Seq[(String, Ast)] = (for {
    (condView, cov) <- target
    CondView(cond, view) = condView
    nearest <- cov.targetCondViews.getOrElse(cond, Map()).getOrElse(view, None)
  } yield Walker(nearest, n).walk(ast).map((name, _)))
    .getOrElse(RandomMutator.default(ast, n, target))

  /** internal walker */
  class Walker(nearest: Nearest, n: Int) extends Util.MultiplicativeListWalker {
    val AstSingleTy(name, rhsIdx, subIdx) = nearest.ty
    override def walk(ast: Syntactic): List[Syntactic] =
      if (
        ast.name == name &&
        ast.rhsIdx == rhsIdx &&
        ast.subIdx == subIdx &&
        ast.loc == Some(nearest.loc)
      )
        TotalWalker(ast, n)
      else
        super.walk(ast)
  }

  /** internal walker that mutates all internal nodes with same prob. */
  object TotalWalker extends Util.AdditiveListWalker {
    var c = 0
    def apply(ast: Syntactic, n: Int): List[Syntactic] =
      val k = Util.simpleAstCounter(ast)
      c = (n - 1) / k + 1
      shuffle(walk(ast)).take(n).toList

    override def walk(ast: Syntactic): List[Syntactic] =
      val mutants = super.walk(ast)
      List.tabulate(c)(_ => synthesizer(ast)) ++ mutants
  }
}
