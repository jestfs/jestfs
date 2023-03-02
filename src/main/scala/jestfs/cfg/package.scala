package jestfs.cfg

import jestfs.cfg.util.*
import jestfs.util.BaseUtils.*

/** CFG elements */
trait CFGElem {
  override def toString: String = toString(true, false)

  /** stringify with options */
  def toString(detail: Boolean = true, location: Boolean = false): String =
    val stringifier = CFGElem.getStringifier(detail, location)
    import stringifier.elemRule
    stringify(this)
}
object CFGElem {
  val getStringifier =
    cached[(Boolean, Boolean), Stringifier] { Stringifier(_, _) }
}
