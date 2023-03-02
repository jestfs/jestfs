package jestfs.lang

import jestfs.lang.util.*

// metalanguage blocks
sealed trait Block extends Syntax
object Block extends Parser.From(Parser.block)

case class StepBlock(steps: List[SubStep]) extends Block
case class ExprBlock(exprs: List[Expression]) extends Block
case class Figure(lines: List[String]) extends Block

// sub-steps with optional id tags
// TODO handle user-effective directive
case class SubStep(idTag: Option[String], step: Step) extends Syntax
