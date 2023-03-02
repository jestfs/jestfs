package jestfs.es.builtin

import jestfs.cfg.CFG
import jestfs.state.*
import scala.collection.mutable.{Map => MMap}

// properties
sealed trait Property {

  /** convert to ir map object */
  def toObject(using CFG): MapObj = this match
    case DataProperty(v, w, e, c) =>
      MapObj("PropertyDescriptor")(
        Str("Value") -> v,
        Str("Writable") -> Bool(w),
        Str("Enumerable") -> Bool(e),
        Str("Configurable") -> Bool(c),
      )
    case AccessorProperty(g, s, e, c) =>
      MapObj("PropertyDescriptor")(
        Str("Get") -> g,
        Str("Set") -> s,
        Str("Enumerable") -> Bool(e),
        Str("Configurable") -> Bool(c),
      )

}

// data properties
case class DataProperty(
  value: PureValue,
  writable: Boolean,
  enumerable: Boolean,
  configurable: Boolean,
) extends Property

// accessor properties
case class AccessorProperty(
  get: PureValue,
  set: PureValue,
  enumerable: Boolean,
  configurable: Boolean,
) extends Property
