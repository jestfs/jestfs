package jestfs.es.util.synthesizer

import jestfs.cfg.*
import jestfs.error.*
import jestfs.es.*
import jestfs.spec.*
import jestfs.util.BaseUtils.*

/** A weighted ECMAScript AST synthesizer */
class WeightedSynthesizer(
  val pairs: (Synthesizer, Int)*,
) extends Synthesizer {

  /** synthesizer name */
  def name: String = "WeightedSynthesizer"

  /** get script */
  def script: String = weightedChoose(pairs).script
  private lazy val array: Array[(Synthesizer, Int)] = pairs.toArray

  /** get initial pool */
  lazy val initPool: Vector[String] = (for {
    (syn, _) <- pairs
    code <- syn.initPool
  } yield code).toVector

  /** for syntactic production */
  def apply(name: String, args: List[Boolean]): Syntactic =
    throw NotSupported("WeightedSynthesizer.apply")

  /** for lexical production */
  def apply(name: String): Lexical =
    throw NotSupported("WeightedSynthesizer.apply")
}
