package jestfs.util

import jestfs.LINE_SEP
import scala.annotation.alpha

/** A trait for objects that have a location in spec.html */
trait Locational {

  /** source location */
  var loc: Option[Loc] = None

  /** set source location with start and end positions and steps */
  def setLoc(start: Pos, end: Pos, steps: List[Int]): this.type =
    setLoc(Some(Loc(start, end, steps)))

  /** set source location */
  def setLoc(locOpt: Option[Loc]): this.type =
    if (loc.isEmpty) loc = locOpt
    this
}

/** source locations in algorithms
  *
  * @example
  *   3:2-4:7 (1.2.2) for `Loc(Pos(3,2), Pos(4,7), List(1,2,2,))`
  */
case class Loc(
  var start: Pos,
  var end: Pos,
  var steps: List[Int],
) {

  /** get substring from a string */
  def getString(str: String): String = str.substring(start.offset, end.offset)

  /** get range string */
  def rangeString: String =
    val (Pos(sl, sc, _), Pos(el, ec, _)) = (start, end)
    if (sl == el) s"$sl:$sc-$ec" else s"$sl:$sc - $el:$ec"

  /** get step string */
  def stepString: String =
    (for ((step, idx) <- steps.zipWithIndex) yield idx % 3 match
      case 0 => step.toString
      case 1 => AlphabetNumeral(step)
      case 2 => RomanNumeral(step, lower = true)
    ).mkString(".")

  /** conversion to string */
  override def toString: String =
    if (stepString == "") s"($rangeString)"
    else
      s"(step $stepString, $rangeString)"
}

/** positions in algorithms
  *
  * @example
  *   3:2 for `Pos(3,2)`
  */
case class Pos(
  var line: Int,
  var column: Int,
  var offset: Int,
) {

  /** get simple string */
  def simpleString: String = s"$line:$column"

  /** append another position */
  def +(that: Pos): Pos =
    val newLine = this.line + that.line - 1
    val newColumn = (if (that.line == 1) this.column else 0) + that.column
    val newOffset = this.offset + that.offset
    Pos(newLine, newColumn, newOffset)

  /** append a source location */
  def +(loc: Loc): Loc = Loc(this + loc.start, this + loc.end, loc.steps)

  /** conversion to string */
  override def toString: String = s"$simpleString($offset)"
}
