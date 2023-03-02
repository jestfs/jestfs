package jestfs.ir

import jestfs.ir.util.Parser
import jestfs.spec.{Param => SpecParam}

/** IR function parameters */
case class Param(
  lhs: Name,
  ty: Type = UnknownType,
  optional: Boolean = false,
  specParam: Option[SpecParam] = None,
) extends IRElem
object Param extends Parser.From(Parser.param)
