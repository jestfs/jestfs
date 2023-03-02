package jestfs.ty

import jestfs.cfg.Func
import jestfs.state.*
import jestfs.util.*

/** value types */
case class ValueTy(
  comp: CompTy,
  pureValue: PureValueTy,
  subMap: SubMapTy,
) extends Ty
  with Lattice[ValueTy] {
  import ValueTy.*

  /** top check */
  def isTop: Boolean =
    if (this eq Top) true
    else if (this eq Bot) false
    else
      (
        this.comp.isTop &&
        this.pureValue.isTop &&
        this.subMap.isTop
      )

  /** bottom check */
  def isBottom: Boolean =
    if (this eq Bot) true
    else if (this eq Top) false
    else
      (
        this.comp.isBottom &&
        this.pureValue.isBottom &&
        this.subMap.isBottom
      )

  /** partial order/subset operator */
  def <=(that: => ValueTy): Boolean = (this eq that) || (
    this.comp <= that.comp &&
    this.pureValue <= that.pureValue &&
    this.subMap <= that.subMap
  )

  /** union type */
  def ||(that: => ValueTy): ValueTy =
    if (this eq that) this
    else
      ValueTy(
        this.comp || that.comp,
        this.pureValue || that.pureValue,
        this.subMap || that.subMap,
      )

  /** intersection type */
  def &&(that: => ValueTy): ValueTy =
    if (this eq that) this
    else
      ValueTy(
        this.comp && that.comp,
        this.pureValue && that.pureValue,
        this.subMap && that.subMap,
      )

  /** prune type */
  def --(that: => ValueTy): ValueTy =
    if (that.isBottom) this
    else
      ValueTy(
        this.comp -- that.comp,
        this.pureValue -- that.pureValue,
        this.subMap -- that.subMap,
      )

  /** completion check */
  def isCompletion: Boolean =
    !comp.isBottom &&
    pureValue.isBottom &&
    subMap.isBottom

  /** getters */
  def normal: PureValueTy = comp.normal
  def abrupt: BSet[String] = comp.abrupt
  def clo: BSet[String] = pureValue.clo
  def cont: BSet[Int] = pureValue.cont
  def name: NameTy = pureValue.name
  def record: RecordTy = pureValue.record
  def list: ListTy = pureValue.list
  def symbol: Boolean = pureValue.symbol
  def astValue: AstValueTy = pureValue.astValue
  def nt: BSet[Nt] = pureValue.nt
  def codeUnit: Boolean = pureValue.codeUnit
  def const: BSet[String] = pureValue.const
  def math: BSet[BigDecimal] = pureValue.math
  def number: BSet[Number] = pureValue.number
  def bigInt: Boolean = pureValue.bigInt
  def str: BSet[String] = pureValue.str
  def bool: BoolTy = pureValue.bool
  def undef: Boolean = pureValue.undef
  def nullv: Boolean = pureValue.nullv
  def absent: Boolean = pureValue.absent
}
object ValueTy {
  def apply(
    comp: CompTy = CompTy.Bot,
    normal: PureValueTy = PureValueTy.Bot,
    abrupt: BSet[String] = Fin(),
    pureValue: PureValueTy = PureValueTy.Bot,
    clo: BSet[String] = Fin(),
    cont: BSet[Int] = Fin(),
    name: NameTy = NameTy.Bot,
    record: RecordTy = RecordTy.Bot,
    list: ListTy = ListTy.Bot,
    symbol: Boolean = false,
    astValue: AstValueTy = AstValueTy.Bot,
    nt: BSet[Nt] = Fin(),
    codeUnit: Boolean = false,
    const: BSet[String] = Fin(),
    math: BSet[BigDecimal] = Fin(),
    number: BSet[Number] = Fin(),
    bigInt: Boolean = false,
    str: BSet[String] = Fin(),
    bool: BoolTy = BoolTy.Bot,
    undef: Boolean = false,
    nullv: Boolean = false,
    absent: Boolean = false,
    subMap: SubMapTy = SubMapTy.Bot,
  ): ValueTy = ValueTy(
    comp = comp || CompTy(normal, abrupt),
    pureValue = pureValue || PureValueTy(
      clo,
      cont,
      name,
      record,
      list,
      symbol,
      astValue,
      nt,
      codeUnit,
      const,
      math,
      number,
      bigInt,
      str,
      bool,
      undef,
      nullv,
      absent,
    ),
    subMap = subMap,
  )
  val Top: ValueTy = ValueTy(CompTy.Top, PureValueTy.Top, SubMapTy.Top)
  val Bot: ValueTy = ValueTy()
}
