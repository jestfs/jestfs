package jestfs.util

import jestfs.phase.ArgRegex
import jestfs.error.*

/** option kinds
  *
  * @tparam T
  *   the parsing result type
  */
sealed abstract class OptionKind[T] {

  /** postfix string */
  def postfix: String

  /** a list of argument regular expressions */
  def argRegexList(name: String): List[ArgRegex[T]]
}

/** boolean options */
case class BoolOption[T](assign: T => Unit) extends OptionKind[T] {
  def postfix: String = ""
  def argRegexList(name: String): List[ArgRegex[T]] = List(
    (("-" + name).r, "".r, (c, _) => assign(c)),
    (("-" + name + "=").r, ".*".r, (c, _) => throw ExtraArgError(name)),
  )
}

/** number options */
case class NumOption[T](assign: (T, Int) => Unit) extends OptionKind[T] {
  def postfix: String = "={number}"
  def argRegexList(name: String): List[ArgRegex[T]] = List(
    (("-" + name + "=").r, "-?[0-9]+".r, (c, s) => assign(c, s.toInt)),
    (("-" + name + "=").r, ".*".r, (_, _) => throw NoNumArgError(name)),
    (("-" + name).r, "".r, (_, _) => throw NoNumArgError(name)),
  )
}

/** string options */
case class StrOption[T](assign: (T, String) => Unit) extends OptionKind[T] {
  def postfix: String = "={string}"
  def argRegexList(name: String): List[ArgRegex[T]] = List(
    (
      ("-" + name + "=").r,
      "\".+\"".r,
      (c, s) => assign(c, s.substring(1).init),
    ),
    (("-" + name + "=").r, "\"\"".r, (_, _) => throw NoStrArgError(name)),
    (("-" + name + "=").r, ".+".r, (c, s) => assign(c, s)),
    (("-" + name + "=").r, ".*".r, (_, _) => throw NoStrArgError(name)),
    (("-" + name).r, "".r, (_, _) => throw NoStrArgError(name)),
  )
}
