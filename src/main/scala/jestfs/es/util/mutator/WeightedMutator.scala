package jestfs.es.util.mutator

import jestfs.es.*
import jestfs.es.util.synthesizer.*
import jestfs.es.util.{Walker => AstWalker, *}
import jestfs.es.util.Coverage.*
import jestfs.spec.Grammar
import jestfs.util.*
import jestfs.util.BaseUtils.*
import jestfs.ty.AstSingleTy

/** A nearest ECMAScript AST mutator */
class WeightedMutator(
  val pairs: (Mutator, Int)*,
) extends Mutator {

  /** mutate programs */
  def apply(
    ast: Ast,
    n: Int,
    target: Option[(CondView, Coverage)],
  ): Seq[(String, Ast)] = weightedChoose(pairs)(ast, n, target)

  val names = pairs.toList.flatMap(_._1.names).sorted.distinct
}
