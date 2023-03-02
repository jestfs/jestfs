package jestfs.es

import jestfs.es.util.*
import jestfs.spec.Grammar
import jestfs.util.BaseUtils.*

/** ECMAScript elements */
trait ESElem {
  override def toString: String = toString()

  /** stringify with grammar and source text */
  def toString(grammar: Grammar): String = toString(grammar, None)
  def toString(grammar: Grammar, text: String): String =
    toString(grammar, Some(text))
  def toString(
    grammar: Grammar,
    text: Option[String],
  ): String = {
    val stringifier = ESElem.getStringifier(true, false, Some(grammar), text)
    import stringifier.elemRule
    stringify(this)
  }

  /** stringify with options but without grammar */
  def toString(
    detail: Boolean = true,
    location: Boolean = false,
  ): String = {
    val stringifier = ESElem.getStringifier(detail, location, None, None)
    import stringifier.elemRule
    stringify(this)
  }
}
object ESElem {
  val getStringifier =
    cached[(Boolean, Boolean, Option[Grammar], Option[String]), Stringifier] {
      Stringifier(_, _, _, _)
    }
}
