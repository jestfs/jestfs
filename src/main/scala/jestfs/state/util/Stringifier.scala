package jestfs.state.util

import jestfs.cfg.*
import jestfs.state.{*, given}
import jestfs.ir.{Func => IRFunc, *, given}
import jestfs.es.*
import jestfs.util.Appender.{given, *}

/** stringifier for state elements */
class Stringifier(detail: Boolean, location: Boolean) {
  // load IR Stringifier
  val irStringifier = IRElem.getStringifier((detail, location))
  import irStringifier.{given, *}

  // load CFG Stringifier
  val cfgStringifier = CFGElem.getStringifier((detail, location))
  import cfgStringifier.{given, *}

  // load ECMAScript Stringifier
  val esStringifier = ESElem.getStringifier((false, location, None, None))
  import esStringifier.{given, *}

  // elements
  given elemRule: Rule[StateElem] = (app, elem) =>
    elem match
      case elem: State       => stRule(app, elem)
      case elem: Context     => ctxtRule(app, elem)
      case elem: Cursor      => cursorRule(app, elem)
      case elem: CallContext => callCtxtRule(app, elem)
      case elem: Heap        => heapRule(app, elem)
      case elem: Obj         => objRule(app, elem)
      case elem: Value       => valueRule(app, elem)
      case elem: RefValue    => refValRule(app, elem)
      case elem: Provenance  => provenanceRule(app, elem)
      case elem: Feature     => featureRule(app, elem)
      case elem: CallGraph   => callGraphRule(app, elem)
      case elem: CallPath    => callPathRule(app, elem)

  // states
  given stRule: Rule[State] = (app, st) =>
    app.wrap {
      app :> "context: " >> st.context
      given Rule[List[String]] = iterableRule("[", ", ", "]")
      app :> "call-stack: "
      app.wrapIterable("[", "]")(st.callStack)
      app :> "globals: " >> st.globals
      app :> "heap: " >> st.heap
    }

  // contexts
  given ctxtRule: Rule[Context] = (app, ctxt) =>
    app.wrap {
      app :> "cursor: " >> ctxt.cursor >> " @ " >> ctxt.name
      app :> "local-vars: " >> ctxt.locals
      ctxt.retVal.map(app :> "return: " >> _)
    }

  // cursor
  given cursorRule: Rule[Cursor] = (app, cursor) =>
    cursor match
      case NodeCursor(func, node, idx) =>
        app >> func.simpleString
        app >> ":" >> node.simpleString
        app >> ":" >> idx
        node.loc.fold(app)(app >> " (" >> _ >> ")")
      case ExitCursor(func) =>
        app >> func.simpleString

  // calling contexts
  given callCtxtRule: Rule[CallContext] = (app, callCtxt) =>
    val CallContext(retId, context) = callCtxt
    app >> retId >> " @ " >> context.cursor

  // heaps
  given heapRule: Rule[Heap] = (app, heap) =>
    app >> s"(SIZE = " >> heap.size.toString >> "): " >> heap.map

  // heap elements
  given heapElemRule: Rule[(Obj, Option[Provenance])] = (app, pair) =>
    val (obj, provenance) = pair
    app >> obj
    provenance.fold(app)(app >> " @ " >> _)

  // objects
  given objRule: Rule[Obj] = (app, obj) =>
    obj match
      case map @ MapObj(tname, _, _) =>
        given Ordering[PureValue] = Ordering.by(_.toString)
        app >> "[TYPE = " >> tname >> "] " >> map.pairs
      case ListObj(values) =>
        given Rule[List[Value]] = iterableRule("[", ", ", "]")
        app >> values.toList
      case SymbolObj(desc) => app >> "(Symbol " >> desc >> ")"
      case YetObj(tname, msg) =>
        app >> "(Yet [TYPE = " >> tname >> "] \"" >> msg >> "\")"

  // values
  given valueRule: Rule[Value] = (app, value) =>
    value match
      case comp: Comp      => compRule(app, comp)
      case pure: PureValue => pureValueRule(app, pure)

  // completion values
  given compRule: Rule[Comp] = (app, comp) =>
    comp match
      case NormalComp(value) =>
        app >> "N(" >> value >> ")"
      case Comp(ty, value, target) =>
        app >> "comp[" >> ty
        target.map(app >> "/" >> _)
        app >> "]" >> "(" >> value >> ")"

  // pure values (not completion values)
  given pureValueRule: Rule[PureValue] = (app, value) =>
    value match
      case addr: Addr      => addrRule(app, addr)
      case clo: Clo        => cloRule(app, clo)
      case cont: Cont      => contRule(app, cont)
      case AstValue(ast)   => app >> ast
      case gr: Nt          => ntRule(app, gr)
      case m: Math         => mathRule(app, m)
      case c: Const        => constRule(app, c)
      case cu: CodeUnit    => cuRule(app, cu)
      case sv: SimpleValue => svRule(app, sv)

  // addresses
  given addrRule: Rule[Addr] = (app, addr) =>
    addr match
      case NamedAddr(name)   => app >> "#" >> name
      case DynamicAddr(long) => app >> "#" >> long.toString

  // closures
  given cloRule: Rule[Clo] = (app, clo) =>
    val Clo(func, captured) = clo
    given Rule[List[(Name, Value)]] = iterableRule("[", ", ", "]")
    app >> "clo<" >> func.irFunc.name
    if (!captured.isEmpty) app >> ", " >> captured.toList
    app >> ">"

  // continuations
  given contRule: Rule[Cont] = (app, cont) =>
    val Cont(func, captured, _) = cont
    given Rule[List[(Name, Value)]] = iterableRule("[", ", ", "]")
    app >> "cont<" >> func.irFunc.name
    if (!captured.isEmpty) app >> ", " >> captured.toList
    app >> ">"

  // nonterminals
  given ntRule: Rule[Nt] = (app, gr) =>
    given Rule[Boolean] = (app, bool) => app >> (if (bool) "T" else "F")
    given Rule[List[Boolean]] = iterableRule()
    app >> "|" >> gr.name >> "|"
    if (!gr.params.isEmpty) app >> "[" >> gr.params >> "]"
    app

  // math
  given mathRule: Rule[Math] = (app, math) => app >> math.n

  // constant
  given constRule: Rule[Const] = (app, c) => app >> "~" >> c.name >> "~"

  // code unit
  given cuRule: Rule[CodeUnit] = (app, cu) => app >> cu.c.toInt >> "cu"

  // simple values
  given svRule: Rule[SimpleValue] = (app, sv) =>
    sv match
      case Number(n)  => app >> n >> "f"
      case BigInt(n)  => app >> n >> "n"
      case Str(str)   => app >> "\"" >> str >> "\""
      case Bool(bool) => app >> bool
      case Undef      => app >> "undefined"
      case Null       => app >> "null"
      case Absent     => app >> "absent"

  // reference value
  lazy val inlineProp = "([_a-zA-Z][_a-zA-Z0-9]*)".r
  given refValRule: Rule[RefValue] = (app, refValue) =>
    refValue match {
      case IdValue(id)                           => app >> id
      case PropValue(base, Str(inlineProp(str))) => app >> base >> "." >> str
      case PropValue(base, prop) => app >> base >> "[" >> prop >> "]"
    }

  // provenance
  given provenanceRule: Rule[Provenance] = (app, provenance) =>
    val Provenance(cursor, sdo) = provenance
    app >> cursor.func.name
    cursor.loc.fold(app)(app >> " (" >> _ >> ")")
    sdo.fold(app)(app >> _)

  // syntax directed operation information
  given featureRule: Rule[Feature] = (app, feature) =>
    val irFunc = feature.func.irFunc
    app >> irFunc.kind >> irFunc.name

  // abstraction of call stack as graph
  given callGraphRule: Rule[CallGraph] = (app, graph) =>
    val CallGraph(start, edges, end) = graph
    given Rule[Call] = (app, call) => app >> call.id
    app >> "CallGraph[" >> start
    if (!edges.isEmpty)
      given Rule[Iterable[(Call, Call)]] = iterableRule("{", ", ", "}")
      app >> " -> " >> edges.toList.sortBy(_._1.id) >> " -> " >> end
    app >> "]"

  // abstraction of call stack as simple path
  given callPathRule: Rule[CallPath] = (app, path) =>
    given Rule[Call] = (app, call) => app >> call.id
    given Rule[Iterable[Call]] = iterableRule("[", " <- ", "]")
    app >> "CallPath" >> path.path
}
