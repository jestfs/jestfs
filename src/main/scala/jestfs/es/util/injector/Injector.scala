package jestfs.es.util.injector

import jestfs.INJECT_LOG_DIR
import jestfs.cfg.CFG
import jestfs.interpreter.Interpreter
import jestfs.ir.*
import jestfs.es.*
import jestfs.es.util.*
import jestfs.spec.*
import jestfs.state.*
import jestfs.util.*
import jestfs.util.BaseUtils.*
import jestfs.util.SystemUtils.*
import jestfs.{LINE_SEP, RESOURCE_DIR}
import java.io.PrintWriter
import scala.collection.mutable.{Map => MMap, ListBuffer}

/** assertion injector */
object Injector:
  def apply(
    src: String,
    defs: Boolean = false,
    log: Boolean = false,
  ): ConformTest =
    val extractor = ExitStateExtractor(cfg.init.from(src))
    new Injector(extractor.initSt, extractor.result, defs, log).conformTest

  /** injection from files */
  def fromFile(
    filename: String,
    defs: Boolean = false,
    log: Boolean = false,
  ): ConformTest =
    val extractor = ExitStateExtractor(cfg.init.fromFile(filename))
    new Injector(extractor.initSt, extractor.result, defs, log).conformTest

  /** assertion definitions */
  lazy val assertionLib: String = readFile(s"$RESOURCE_DIR/injector/lib.js")

/** extensible helper of assertion injector */
class Injector(
  initSt: State,
  exitSt: State,
  defs: Boolean,
  log: Boolean,
) {

  /** generated assertions */
  lazy val assertions: Vector[Assertion] =
    _assertions.clear()
    if (normalExit)
      handleVariable // inject assertions from variables
      handleLet // inject assertions from lexical variables
    if (log)
      pw.close
      println("[Injector] Logging finished")
    _assertions.toVector

  /** generated conformance test */
  lazy val conformTest: ConformTest =
    ConformTest(
      0,
      script,
      exitTag,
      defs,
      isAsync,
      assertions,
    )

  /** injected script */
  lazy val result: String = conformTest.toString

  /** target script */
  lazy val script = initSt.sourceText.get

  /** exit status tag */
  lazy val exitTag: ExitTag = ExitTag(exitSt)

  /** normal termination */
  lazy val normalExit: Boolean = exitTag == NormalTag

  /** check whether it uses asynchronous features */
  // TODO more precise detection
  lazy val isAsync: Boolean =
    script.contains("async") || script.contains("Promise")

  // ---------------------------------------------------------------------------
  // private helpers
  // ---------------------------------------------------------------------------
  // logging
  private lazy val pw: PrintWriter =
    println(s"[Injector] Logging into $INJECT_LOG_DIR...")
    mkdir(INJECT_LOG_DIR)
    getPrintWriter(s"$INJECT_LOG_DIR/log")

  private def log(data: Any): Unit = if (log) {
    pw.println(data);
    pw.flush()
  }

  private def warning(msg: String): Unit = log(s"[Warning] $msg")

  // internal mutable assertions
  private val _assertions: ListBuffer[Assertion] = ListBuffer()

  // handle variables
  private def handleVariable: Unit = for (x <- createdVars.toList.sorted) {
    log("handling variable...")
    val path = s"globalThis[\"$x\"]"
    getValue(s"""$globalMap["$x"].Value""") match
      case Absent => /* do nothing(handle global accessor property) */
      case sv: SimpleValue =>
        _assertions += HasValue(path, sv)
      case addr: Addr => handleObject(addr, path)
      case _          => /* do nothing */
  }

  // get created variables
  private lazy val globalMap = "@REALM.GlobalObject.SubMap"
  private lazy val globalThis =
    getValue(s"$globalMap.globalThis.Value")
  private lazy val createdVars: Set[String] =
    val initial = getStrKeys(getValue("@GLOBAL.SubMap"), "<global>")
    val current = getStrKeys(getValue(globalMap), "<global>")
    (current -- initial)

  // handle lexical variables
  private def handleLet: Unit = for (x <- createdLets.toList.sorted) {
    log("handling let...")
    getValue(s"""$lexRecord["$x"].BoundValue""") match
      case sv: SimpleValue => _assertions += HasValue(x, sv)
      case addr: Addr      => handleObject(addr, x)
      case _               => /* do nothing */
  }

  // handle addresses
  private def handleObject(addr: Addr, path: String): Unit =
    log(s"handleObject: $addr, $path")
    (addr, handledObjects.get(addr)) match
      case (_, Some(origPath)) =>
        _assertions += SameObject(addr, path, origPath)
      case (_: DynamicAddr, None) if addr != globalThis =>
        handledObjects += addr -> path
        exitSt(addr) match
          case (_: MapObj) =>
            handlePrototype(addr, path)
            handleExtensible(addr, path)
            handleCall(addr, path)
            handleConstruct(addr, path)
            handlePropKeys(addr, path)
            handleProperty(addr, path)
          case _ =>
      case _ =>

  private var handledObjects: Map[Addr, String] = (for {
    addr <- initSt.heap.map.keySet
    name <- addrToName(addr)
  } yield addr -> name).toMap
  private lazy val PREFIX_INTRINSIC = "INTRINSICS."

  private def addrToName(addr: Addr): Option[String] = addr match
    case a @ NamedAddr(name) if name.startsWith(PREFIX_INTRINSIC) =>
      Some(name.substring(PREFIX_INTRINSIC.length))
    case _ => None

  // handle [[Prototype]]
  private def handlePrototype(addr: Addr, path: String): Unit =
    log(s"handlePrototype: $addr, $path")
    access(addr, Str("Prototype")) match
      case addr: Addr => handleObject(addr, s"$$Object_getPrototypeOf($path)")
      case _          => warning("non-address [[Prototype]]: $path")

  // handle [[Extensible]]
  private def handleExtensible(addr: Addr, path: String): Unit =
    log(s"handleExtensible: $addr, $path")
    access(addr, Str("Extensible")) match
      case Bool(b) =>
        _assertions += IsExtensible(addr, path, b)
      case _ => warning("non-boolean [[Extensible]]: $path")

  // handle [[Call]]
  private def handleCall(addr: Addr, path: String): Unit =
    log(s"handleCall: $addr, $path")
    _assertions += IsCallable(addr, path, access(addr, Str("Call")) != Absent)

  // handle [[Construct]]
  private def handleConstruct(addr: Addr, path: String): Unit =
    log(s"handleConstruct: $addr, $path")
    _assertions += IsConstructable(
      addr,
      path,
      access(addr, Str("Construct")) != Absent,
    )

  // handle property names
  private def handlePropKeys(addr: Addr, path: String): Unit =
    log(s"handlePropKeys: $addr, $path")
    val newSt = exitSt.copied
    getValue(addr, "OwnPropertyKeys") match
      case Clo(f, _) =>
        newSt.context = Context(f, MMap(Name("O") -> addr))
        newSt.callStack = Nil
        Interpreter(newSt)
        val propsAddr = newSt(GLOBAL_RESULT) match
          case Comp(_, addr: Addr, _) => addr
          case addr: Addr             => addr
          case v                      => error("not an address: $v")
        val len = newSt(propsAddr, Str("length")).asMath.toInt
        val array = (0 until len)
          .map(k => newSt(propsAddr, Math(k)))
          .flatMap(_ match {
            case Str(str)   => Some(s"'$str'")
            case addr: Addr => addrToName(addr)
            case _          => None
          })
        if (array.length == len)
          _assertions += CompareArray(addr, path, array)
      case _ => warning("non-closure [[OwnPropertyKeys]]: $path")

  // handle properties
  private lazy val fields =
    List("Get", "Set", "Value", "Writable", "Enumerable", "Configurable")

  private def handleProperty(addr: Addr, path: String): Unit =
    log(s"handleProperty: $addr, $path")
    val subMap = access(addr, Str("SubMap"))
    for (p <- getKeys(subMap, path)) access(subMap, p) match
      case addr: Addr =>
        exitSt(addr) match
          case MapObj(
                "PropertyDescriptor" | "DataProperty" | "AccessorProperty",
                props,
                _,
              ) =>
            var desc = Map[String, SimpleValue]()
            val2str(p).foreach(propStr => {
              for {
                field <- fields
                value <- props.get(Str(field)).map(_.value.toPureValue)
              } value match
                case sv: SimpleValue =>
                  desc += (field.toLowerCase -> sv)
                case addr: Addr =>
                  field match
                    case "Value" => handleObject(addr, s"$path?.[$propStr]")
                    case "Get" =>
                      handleObject(
                        addr,
                        s"Object.getOwnPropertyDescriptor($path, $propStr)?.get",
                      )
                    case "Set" =>
                      handleObject(
                        addr,
                        s"Object.getOwnPropertyDescriptor($path, $propStr)?.set",
                      )
                    case _ =>
                case _ => warning("invalid property: $path")
              _assertions += VerifyProperty(addr, path, propStr, desc)
            })
          case x => warning("invalid property: $path")
      case v => warning("invalid property: $path")

  // get values
  private def getValue(str: String): Value = getValue(Expr.from(str))

  private def getValue(expr: Expr): Value =
    (new Interpreter(exitSt.copied)).eval(expr)

  private def getValue(refV: RefValue): Value = exitSt(refV)

  private def getValue(addr: Addr, prop: String): Value =
    getValue(PropValue(addr, Str(prop)))

  // access properties
  private def access(base: Value, props: PureValue*): Value =
    props.foldLeft(base) { case (base, p) => exitSt(base, p) }

  // get created lexical variables
  private lazy val lexRecord =
    "@REALM.GlobalEnv.DeclarativeRecord.SubMap"
  private lazy val createdLets: Set[String] =
    getStrKeys(getValue(lexRecord), "<global-decl-record>")

  // get keys
  private def getStrKeys(value: Value, path: String): Set[String] =
    getKeys(value, path).collect { case Str(p) => p }

  private def getKeys(value: Value, path: String): Set[PureValue] = value match
    case addr: Addr =>
      exitSt(addr) match
        case m: MapObj => m.props.keySet.toSet
        case _ => warning("[[SubMap]] is not a map object: $path"); Set()
    case _ => warning("[[SubMap]] is not an address: $path"); Set()

  // conversion to ECMAScript code
  private def val2str(value: Value): Option[String] = value match
    case sv: SimpleValue => Some(sv.toString)
    case addr: Addr      => addrToName(addr)
    case x               => None

  private def sv2str(sv: SimpleValue): String = sv match
    case Number(n) => n.toString
    case v         => v.toString
}
