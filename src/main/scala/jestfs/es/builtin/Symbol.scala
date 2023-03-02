package jestfs.es.builtin

import jestfs.cfg.CFG
import jestfs.state.*
import jestfs.ty.*

/** model for symbols */
case class Symbol(cfg: CFG) {
  private def spec = cfg.program.spec
  given CFG = cfg

  private lazy val symbols: List[String] = (for {
    row <- spec.tables(WELL_KNOWN_SYMBOLS).rows
    symbolKey <- row.headOption.map(_.stripPrefix("@@"))
  } yield symbolKey)

  /** get symbol record */
  def ty: ValueTy = RecordT(
    (for (symKey <- symbols) yield symKey -> SymbolT): _*,
  )

  /** get symbol record */
  def obj: MapObj = MapObj("Record")(
    (for { symKey <- symbols } yield Str(symKey) -> symbolAddr(symKey)): _*,
  )

  /** get map for heap */
  def map: Map[Addr, Obj] = (for { symKey <- symbols } yield symbolAddr(
    symKey,
  ) -> SymbolObj(Str(symbolName(symKey)))).toMap
}
