package jestfs.state

import jestfs.cfg.*
import jestfs.error.*
import jestfs.ir.{Func => IRFunc, *}
import jestfs.es.builtin.*
import jestfs.util.BaseUtils.*
import scala.collection.mutable.{Map => MMap}

/** IR heaps */
class Heap(
  val map: MMap[Addr, (Obj, Option[Provenance])] = MMap(),
  var size: Int = 0,
) extends StateElem {

  /** getters */
  def apply(addr: Addr): Obj =
    map.getOrElse(addr, throw UnknownAddr(addr)) match
      case (YetObj(_, msg), _) => throw NotSupported(msg)
      case (obj, _)            => obj
  def apply(addr: Addr, key: PureValue): Value = apply(addr) match
    case _ if addr == NamedAddr(INTRINSICS) => Heap.getIntrinsics(key)
    case (s: SymbolObj)                     => s(key)
    case (m: MapObj)                        => m(key)
    case (l: ListObj)                       => l(key)
    case YetObj(_, msg)                     => throw NotSupported(msg)

  /** get provenance */
  def getProvenance(value: Value): Option[Provenance] = value match
    case NormalComp(value) => getProvenance(value)
    case addr: Addr        => map.get(addr).flatMap { case (_, prov) => prov }
    case _                 => None

  /** setters */
  def update(addr: Addr, prop: PureValue, value: Value): this.type =
    apply(addr) match {
      case (m: MapObj)  => m.update(prop, value); this
      case (l: ListObj) => l.update(prop, value); this
      case v            => error(s"not a map: $v")
    }

  /** delete */
  def delete(addr: Addr, prop: PureValue): this.type = apply(addr) match {
    case (m: MapObj) => m.delete(prop); this
    case v           => error(s"not a map: $v")
  }

  /** appends */
  def append(addr: Addr, value: PureValue): this.type = apply(addr) match {
    case (l: ListObj) => l.append(value); this
    case v            => error(s"not a list: $v")
  }

  /** prepends */
  def prepend(addr: Addr, value: PureValue): this.type = apply(addr) match {
    case (l: ListObj) => l.prepend(value); this
    case v            => error(s"not a list: $v")
  }

  /** pops */
  def pop(addr: Addr, front: Boolean): PureValue = apply(addr) match {
    case (l: ListObj) => l.pop(front)
    case v            => error(s"not a list: $v")
  }

  /** remove given elements from list */
  def remove(addr: Addr, value: PureValue): this.type = apply(addr) match {
    case (l: ListObj) => l.remove(value); this
    case v            => error(s"not a list: $v")
  }

  /** copy objects */
  def copyObj(
    addr: Addr,
  )(using Option[Provenance]): Addr = alloc(apply(addr).copied)

  /** keys of map */
  def keys(
    addr: Addr,
    intSorted: Boolean,
  )(using Option[Provenance]): Addr = {
    alloc(ListObj(apply(addr) match {
      case (m: MapObj) => m.keys(intSorted)
      case obj         => error(s"not a map: $obj")
    }))
  }

  /** map allocations */
  def allocMap(
    tname: String,
    m: Map[PureValue, PureValue],
  )(using CFG, Option[Provenance]): Addr = {
    val irMap =
      if (tname == "Record") MapObj(tname, MMap(), 0) else MapObj(tname)
    for ((k, v) <- m) irMap.update(k, v)
    if (hasSubMap(tname))
      val subMap = MapObj("SubMap")
      irMap.update(Str("SubMap"), alloc(subMap))
    if (isObject(tname))
      val privateElems = ListObj()
      irMap.update(Str("PrivateElements"), alloc(privateElems))
    alloc(irMap)
  }

  private def isObject(tname: String): Boolean =
    tname endsWith "Object"
  private def isEnvRec(tname: String): Boolean =
    tname endsWith "EnvironmentRecord"
  private def hasSubMap(tname: String): Boolean =
    isObject(tname) || isEnvRec(tname)

  /** list allocations */
  def allocList(
    list: List[PureValue],
  )(using Option[Provenance]): Addr = alloc(ListObj(list.toVector))

  /** symbol allocations */
  def allocSymbol(
    desc: PureValue,
  )(using Option[Provenance]): Addr = alloc(SymbolObj(desc))

  // allocation helper
  private def alloc(obj: Obj)(using provenance: Option[Provenance]): Addr = {
    val newAddr = DynamicAddr(size)
    map += newAddr -> (obj, provenance)
    size += 1
    newAddr
  }

  // property access helper
  private def getAddrValue(
    addr: Addr,
    propName: String,
  ): Addr = apply(addr, Str(propName)) match {
    case addr: Addr => addr
    case v          => error(s"not an address: $v")
  }

  // property value getter
  private def getPropValue(
    addr: Value,
    propName: String,
  ): Value = addr match {
    case addr: Addr =>
      val submap = getAddrValue(addr, "SubMap")
      val prop = getAddrValue(submap, propName)
      apply(prop, Str("Value"))
    case _ => error(s"not an address: $addr")
  }

  /** set type of objects */
  def setType(addr: Addr, tname: String): this.type = apply(addr) match {
    case (irMap: MapObj) =>
      irMap.ty = tname; this
    case _ => error(s"invalid type update: $addr")
  }

  /** copied */
  def copied: Heap =
    val newMap = MMap.from(map.toList.map {
      case (addr, (obj, provenance)) => addr -> (obj.copied, provenance)
    })
    new Heap(newMap, size)
}
object Heap {
  def apply(
    map: MMap[Addr, Obj] = MMap(),
    size: Int = 0,
  ): Heap = new Heap(map.map { case (addr, obj) => addr -> (obj, None) }, size)

  /** special getter for intrinsics */
  def getIntrinsics(key: PureValue): Value =
    val keyStr = key match
      case Str(s) if s.startsWith("%") && s.endsWith("%") =>
        s.substring(1, s.length - 1)
      case v => error(s"invalid intrinsics key1: $key")
    intrAddr(keyStr)
}
