package jestfs.state.util

import jestfs.state.*
import jestfs.ir.*
import jestfs.util.BasicUnitWalker

/** unit walker for state */
trait UnitWalker extends BasicUnitWalker {
  // all cases
  def walk(elem: StateElem): Unit = elem match
    case elem: State       => walk(elem)
    case elem: Context     => walk(elem)
    case elem: Cursor      => walk(elem)
    case elem: CallContext => walk(elem)
    case elem: Heap        => walk(elem)
    case elem: Obj         => walk(elem)
    case elem: Value       => walk(elem)
    case elem: RefValue    => walk(elem)
    case elem: Provenance  => walk(elem)
    case elem: Feature     => walk(elem)

  // states
  def walk(st: State): Unit =
    val State(_, context, _, _, callStack, globals, heap) = st
    walk(context)
    walkList(callStack, walk)
    walkMMap(globals, walk, walk)
    walk(heap)

  // context
  def walk(context: Context): Unit =
    walkMMap(context.locals, walk, walk)
    walkOpt(context.retVal, walk)

  // cursor
  def walk(cursor: Cursor): Unit = {}

  // calling contexts
  def walk(callContext: CallContext): Unit = walk(callContext.context)

  // heap
  def walk(heap: Heap): Unit = walkMMap(heap.map, walk, walk)

  // heap elements
  def walk(pair: (Obj, Option[Provenance])): Unit =
    val (obj, provenance) = pair
    walk(obj); walkOpt(provenance, walk)

  // object
  def walk(obj: Obj): Unit = obj match
    case MapObj(_, props, _) => walkMMap(props, walk, walk)
    case ListObj(values)     => walkIterable(values, walk)
    case SymbolObj(desc)     => walk(desc)
    case _: YetObj           =>
  def walk(prop: MapObj.Prop): Unit = walk(prop.value)

  // value
  def walk(v: Value): Unit = v match
    case comp: Comp      => walk(comp)
    case pure: PureValue => walk(pure)

  // completion value
  def walk(comp: Comp): Unit =
    walk(comp.ty); walk(comp.value)

  // pure value
  def walk(pure: PureValue): Unit = pure match
    case addr: Addr       => walk(addr)
    case Clo(_, captured) => walkMap(captured, walk, walk)
    case Cont(_, captured, callStack) =>
      walkMap(captured, walk, walk); walkList(callStack, walk)
    case _: AstValue     =>
    case _: Nt           =>
    case _: Math         =>
    case _: Const        =>
    case _: CodeUnit     =>
    case sv: SimpleValue => walk(sv)

  // address
  def walk(addr: Addr): Unit = {}

  // simple value
  def walk(sv: SimpleValue): Unit = {}

  // reference value
  def walk(rv: RefValue): Unit = rv match
    case _: IdValue            =>
    case PropValue(base, prop) => walk(base); walk(prop)

  // ir id
  def walk(id: Id): Unit = {}

  // provenance
  def walk(provenance: Provenance): Unit = {}

  // ECMAScript features
  def walk(feature: Feature): Unit = {}
}
