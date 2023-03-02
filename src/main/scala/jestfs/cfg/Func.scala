package jestfs.cfg

import jestfs.cfg.util.*
import jestfs.ir.{Param, Type, Name, Func => IRFunc, FuncKind, EYet}
import jestfs.spec.Head
import jestfs.ty.*
import jestfs.util.SystemUtils.*
import jestfs.util.BaseUtils.*
import jestfs.util.{Appender, UId}

/** CFG functions */
case class Func(
  id: Int,
  irFunc: IRFunc,
  entry: Node,
) extends CFGElem
  with UId { func =>

  /** parameters */
  lazy val params: List[Param] = irFunc.params

  /** arity */
  lazy val arity: (Int, Int) = irFunc.arity

  /** all types */
  lazy val tys: List[Type] = retTy :: params.map(_.ty)

  /** parameter types */
  lazy val paramTys: List[Type] = params.map(_.ty)

  /** check whether parameter types are defined */
  lazy val isParamTysDefined: Boolean = paramTys.forall(_.isDefined)

  /** not yet supported instructions */
  lazy val yets: List[EYet] = irFunc.yets

  /** check completeness */
  lazy val complete: Boolean = irFunc.complete

  /** return types */
  lazy val retTy: Type = irFunc.retTy

  /** nodes */
  lazy val nodes: Set[Node] = entry.reachable

  /** a mapping from nid to nodes */
  lazy val nodeMap: Map[Int, Node] =
    (for (node <- nodes) yield node.id -> node).toMap

  /** algorithm heads */
  lazy val head: Option[Head] = irFunc.head

  /** algorithm head string */
  def headString: String = irFunc.headString

  /** check whether it is builtin */
  lazy val isBuiltin: Boolean =
    irFunc.kind == FuncKind.Builtin || irFunc.kind == FuncKind.BuiltinClo

  /** check whether it is SDO */
  lazy val isSDO: Boolean = irFunc.kind == FuncKind.SynDirOp

  /** check whether it is method operation */
  lazy val isMethod: Boolean =
    irFunc.kind == FuncKind.ConcMeth || irFunc.kind == FuncKind.InternalMeth

  /** check wheter it needs normal completion wrapping */
  lazy val isReturnComp: Boolean = irFunc.kind match
    case FuncKind.SynDirOp if irFunc.name.endsWith(".Evaluation") => true
    case FuncKind.Builtin | FuncKind.BuiltinClo                   => true
    case _ => irFunc.retTy.isCompletion

  /** function name */
  def name: String = irFunc.name

  /** normalized function name */
  def normalizedName: String = name.replace("/", "").replace("`", "")

  /** conversion to a DOT format */
  def dot: String = toDot()
  def toDot(nid: Int = -1, _isExit: Boolean = false): String = new DotPrinter {
    val isExit: Boolean = _isExit
    def getId(func: Func): String = s"cluster${func.id}"
    def getId(node: Node): String = s"node${node.id}"
    def getName(func: Func): String = func.headString
    def getColor(node: Node): String = REACH
    def getColor(from: Node, to: Node): String = REACH
    def getBgColor(node: Node): String =
      if (node.id == nid) CURRENT else NORMAL
    def apply(app: Appender): Unit = addFunc(func, app)
  }.toString

  /** dump in a DOT format */
  def dumpDot(
    dotPath: String,
    pdfPathOpt: Option[String] = None,
  ): Unit =
    // dump DOT format
    dumpFile(dot, dotPath)

    // dump PDF format
    for {
      pdfPath <- pdfPathOpt
      e <- getError(executeCmd(s"""dot -Tpdf "$dotPath" -o "$pdfPath""""))
    } error(s"""[DOT] [$name]: exception occured while converting to pdf:
               |
               |$e""".stripMargin)

  /** dump in a DOT/PDF format */
  def dumpDot(
    dotPath: String,
    pdfPath: String,
  ): Unit = dumpDot(dotPath, Some(pdfPath))
}
