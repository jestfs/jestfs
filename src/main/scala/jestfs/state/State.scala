package jestfs.state

import jestfs.cfg.*
import jestfs.error.*
import jestfs.ir.{Func => IRFunc, *}
import jestfs.es.*
import jestfs.util.BaseUtils.*
import scala.collection.mutable.{Map => MMap}

/** IR states */
case class State(
  val cfg: CFG,
  var context: Context,
  val sourceText: Option[String] = None,
  val cachedAst: Option[Ast] = None,
  var callStack: List[CallContext] = Nil,
  val globals: MMap[Global, Value] = MMap(),
  val heap: Heap = Heap(),
) extends StateElem {

  /** get the current function */
  def func: Func = context.cursor.func

  /** get local variable maps */
  def locals: MMap[Local, Value] = context.locals

  /** lookup variable directly */
  def directLookup(x: Id): Value = (x match {
    case x: Global => globals.get(x)
    case x: Local  => context.locals.get(x)
  }).getOrElse(throw UnknownId(x))

  /** getters */
  def apply(refV: RefValue): Value = refV match
    case IdValue(x)            => apply(x)
    case PropValue(base, prop) => apply(base, prop)
  def apply(x: Id): Value = directLookup(x) match
    case Absent if func.isBuiltin => Undef
    case v                        => v
  def apply(base: Value, prop: PureValue): Value = base match
    case comp: Comp =>
      prop match
        case Str("Type")   => comp.ty
        case Str("Value")  => comp.value
        case Str("Target") => comp.targetValue
        case _             => throw InvalidCompProp(comp, prop)
    case addr: Addr    => heap(addr, prop)
    case AstValue(ast) => apply(ast, prop)
    case Str(str)      => apply(str, prop)
    case v             => throw InvalidRefBase(v)
  def apply(ast: Ast, prop: PureValue): PureValue =
    (ast, prop) match
      case (_, Str("parent")) => ast.parent.map(AstValue(_)).getOrElse(Absent)
      case (syn: Syntactic, Str(propStr)) =>
        val Syntactic(name, _, rhsIdx, children) = syn
        val rhs = cfg.grammar.nameMap(name).rhsVec(rhsIdx)
        rhs.getNtIndex(propStr).flatMap(children(_)) match
          case Some(child) => AstValue(child)
          case _           => throw InvalidAstProp(syn, Str(propStr))
      case (syn: Syntactic, Math(n)) if n.isValidInt =>
        syn.children(n.toInt).map(AstValue(_)).getOrElse(Absent)
      case _ => throw InvalidAstProp(ast, prop)
  def apply(str: String, prop: PureValue): PureValue = prop match
    case Str("length") => Math(BigDecimal.exact(str.length))
    case Math(k)       => CodeUnit(str(k.toInt))
    case Number(k)     => CodeUnit(str(k.toInt))
    case _             => throw WrongStringRef(str, prop)
  def apply(addr: Addr): Obj = heap(addr)

  /** get provenance */
  def getProvenance(value: Value): Option[Provenance] =
    heap.getProvenance(value)

  /** setters */
  def define(x: Id, value: Value): this.type = x match
    case x: Global => globals += x -> value; this
    case x: Local  => context.locals += x -> value; this
  def update(refV: RefValue, value: Value): this.type = refV match {
    case IdValue(x) => update(x, value); this
    case PropValue(base, prop) =>
      base match
        // XXX see https://github.com/es-meta/jestfs/issues/65
        case comp: Comp if comp.isAbruptCompletion && prop.asStr == "Value" =>
          comp.value = value.toPureValue; this
        case addr: Addr => update(addr, prop, value); this
        case _          => error(s"illegal reference update: $refV = $value")
  }
  def update(x: Id, value: Value): this.type =
    x match
      case x: Global if hasBinding(x) => globals += x -> value
      case x: Name if hasBinding(x)   => context.locals += x -> value
      case x: Temp                    => context.locals += x -> value
      case _ => error(s"illegal variable update: $x = $value")
    this
  def update(addr: Addr, prop: PureValue, value: Value): this.type =
    heap.update(addr, prop, value); this

  /** existence checks */
  private def hasBinding(x: Id): Boolean = x match
    case x: Global => globals contains x
    case x: Local  => context.locals contains x
  def exists(x: Id): Boolean = hasBinding(x) && directLookup(x) != Absent
  def exists(ref: RefValue): Boolean = ref match {
    case IdValue(id)           => exists(id)
    case PropValue(base, prop) => apply(base, prop) != Absent
  }

  /** delete a property from a map */
  def delete(refV: RefValue): this.type = refV match {
    case IdValue(x) =>
      error(s"cannot delete variable $x")
    case PropValue(base, prop) =>
      base match {
        case addr: Addr =>
          heap.delete(addr, prop); this
        case _ =>
          error(s"illegal reference delete: delete $refV")
      }
  }

  /** object operators */
  def append(addr: Addr, value: PureValue): this.type =
    heap.append(addr, value); this
  def prepend(addr: Addr, value: PureValue): this.type =
    heap.prepend(addr, value); this
  def pop(addr: Addr, front: Boolean): PureValue =
    heap.pop(addr, front)
  def remove(addr: Addr, value: PureValue): this.type =
    heap.remove(addr, value); this
  def copyObj(addr: Addr)(using Option[Provenance]): Addr =
    heap.copyObj(addr)
  def keys(addr: Addr, intSorted: Boolean)(using Option[Provenance]): Addr =
    heap.keys(addr, intSorted)
  def allocMap(
    tname: String,
    map: Map[PureValue, PureValue] = Map(),
  )(using CFG, Option[Provenance]): Addr = heap.allocMap(tname, map)
  def allocList(list: List[PureValue])(using Option[Provenance]): Addr =
    heap.allocList(list)
  def allocSymbol(desc: PureValue)(using Option[Provenance]): Addr =
    heap.allocSymbol(desc)
  def setType(addr: Addr, tname: String): this.type =
    heap.setType(addr, tname); this

  /** get string for a current cursor */
  def getCursorString: String = context.cursor match
    case NodeCursor(func, node, _) =>
      val irFunc = func.irFunc
      s"[${irFunc.kind}${irFunc.name}] ${node.toString(location = true)}"
    case ExitCursor(func) =>
      val irFunc = func.irFunc
      s"[${irFunc.kind}${irFunc.name}] Exited"

  /** get string for a given address */
  def getString(value: Value): String = value match {
    case comp: Comp => comp.toString + getString(comp.value)
    case addr: Addr =>
      val obj = heap(addr)
      val subMapStr = obj.getSubMap.fold("")(s" with " + getString(_))
      s"$addr -> $obj$subMapStr"
    case _ => value.toString
  }

  /** copied */
  def copied: State =
    val newGlobals = MMap.from(globals)
    val newHeap = heap.copied
    val newContext = context.copied
    val newCallStack = callStack.map(_.copied)
    State(
      cfg,
      newContext,
      sourceText,
      cachedAst,
      newCallStack,
      newGlobals,
      newHeap,
    )

  /** provenance of addresses */
  def provenance: Provenance = context.provenance
}
object State {

  /** initialize states with a CFG */
  def apply(cfg: CFG): State = State(cfg, Context(cfg.main))
}
