package jestfs.es.util.synthesizer

import jestfs.cfg.*
import jestfs.es.util.GrammarDiff
import jestfs.spec.{Production, Rhs}

object NewFeatureSynthesizer extends NewFeatureSynthesizer
trait NewFeatureSynthesizer extends RandomSynthesizer {

  /** synthesizer name */
  override val name: String = "NewFeatureSynthesizer"

  override protected def chooseRhs(
    prod: Production,
    pairs: Iterable[(Rhs, Int)],
  ): (Rhs, Int) = GrammarDiff.chooseRhs(prod, pairs)
}
