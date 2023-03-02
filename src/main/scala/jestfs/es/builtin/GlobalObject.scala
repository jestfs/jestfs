package jestfs.es.builtin

import jestfs.cfg.CFG
import jestfs.state.*
import jestfs.spec.*

/** model for global object */
case class GlobalObject(cfg: CFG) {

  /** shortcuts */
  private val T = true
  private val F = false
  private val U = Undef
  private val spec = cfg.program.spec
  given CFG = cfg

  /** get global object */
  def obj: MapObj = MapObj("Object")(Str(SUBMAP) -> submapAddr(GLOBAL))

  /** get map for heap */
  lazy val map: Map[Addr, Obj] = {
    var nmap = List(
      // NOTE: globalThis is added in SetDefaultGlobalBindings
      "print" -> DataProperty(intrAddr("print"), T, F, T),
      "@@toStringTag" -> DataProperty(Str("global"), F, F, T),
      "Infinity" -> DataProperty(Number(Double.PositiveInfinity), F, F, F),
      "NaN" -> DataProperty(Number(Double.NaN), F, F, F),
      "undefined" -> DataProperty(Undef, F, F, F),
      // test262
      "$262" -> DataProperty(intrAddr("$262"), T, F, T),
    )
    for {
      row <- spec.tables(WELL_KNOWN_INTRINSICS).rows
      List(intrCell, globCell) = row.take(2).map(_.trim) if globCell != ""
      intrKey = intrCell.replace("%", "")
      globKey = globCell.replace("`", "")
    } { nmap ::= globKey -> DataProperty(intrAddr(intrKey), T, F, T) }

    getSubmapObjects(GLOBAL, GLOBAL, nmap)
  }
}
