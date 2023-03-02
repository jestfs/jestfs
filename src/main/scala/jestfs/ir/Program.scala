package jestfs.ir

import jestfs.*
import jestfs.ir.util.{Parser, YetCollector}
import jestfs.parser.{ESParser, AstFrom}
import jestfs.spec.Spec
import jestfs.ty.TyModel
import jestfs.util.BaseUtils.*
import jestfs.util.ProgressBar
import jestfs.util.SystemUtils.*

/** IR programs */
case class Program(
  funcs: List[Func] = Nil, // IR functions
) extends IRElem {

  /** backward edge to a specification */
  var spec: Spec = Spec()

  /** the main function */
  lazy val main: Func = getUnique(funcs, _.main, "main function")

  /** ECMAScript parser */
  lazy val esParser: ESParser = spec.esParser
  lazy val scriptParser: AstFrom = esParser("Script")

  /** get list of all yet expressions */
  lazy val yets: List[(EYet, Func)] = for {
    func <- funcs
    yet <- func.yets
  } yield (yet, func)

  /** complete functions */
  lazy val completeFuncs: List[Func] = funcs.filter(_.complete)

  /** incomplete functions */
  lazy val incompleteFuncs: List[Func] = funcs.filter(!_.complete)

  /** get a type model */
  def tyModel: TyModel = spec.tyModel

  /** dump IR program */
  def dumpTo(baseDir: String, loc: Boolean = false): Unit =
    val dirname = s"$baseDir/func"
    dumpDir(
      name = "IR functions",
      iterable = ProgressBar("Dump IR functions", funcs),
      dirname = dirname,
      getName = func => s"${func.normalizedName}.ir",
      getData = func => func.toString(detail = true, location = loc),
    )
}
object Program extends Parser.From(Parser.program) {
  def apply(funcs: List[Func], spec: Spec): Program =
    val program = Program(funcs)
    program.spec = spec
    program
}
