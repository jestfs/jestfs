package jestfs.ty.util

import jestfs.LINE_SEP
import jestfs.state.Number
import jestfs.ty.*
import jestfs.util.*
import jestfs.util.Appender.*
import jestfs.util.BaseUtils.*

/** stringifier for types */
object Stringifier {

  /** type elements */
  given elemRule: Rule[TyElem] = (app, elem) =>
    elem match
      case elem: UnknownTy   => unknownTyRule(app, elem)
      case elem: ValueTy     => valueTyRule(app, elem)
      case elem: CompTy      => compTyRule(app, elem)
      case elem: ListTy      => listTyRule(app, elem)
      case elem: PureValueTy => pureValueTyRule(app, elem)
      case elem: NameTy      => nameTyRule(app, elem)
      case elem: RecordTy    => recordTyRule(app, elem)
      case elem: AstValueTy  => astValueTyRule(app, elem)
      case elem: SubMapTy    => subMapTyRule(app, elem)
      case elem: BoolTy      => boolTyRule(app, elem)

  /** types */
  given tyRule: Rule[Ty] = (app, ty) =>
    ty match
      case ty: UnknownTy => unknownTyRule(app, ty)
      case ty: ValueTy   => valueTyRule(app, ty)

  /** unknown types */
  given unknownTyRule: Rule[UnknownTy] = (app, ty) =>
    app >> "Unknown"
    ty.msg.fold(app)(app >> "[\"" >> _ >> "\"]")

  /** value types */
  given valueTyRule: Rule[ValueTy] = (app, ty) =>
    if (ty.isTop) app >> "Any"
    else if (!ty.isBottom)
      FilterApp(app)
        .add(ty.comp, !ty.comp.isBottom)
        .add(ty.pureValue, !ty.pureValue.isBottom)
        .add(ty.subMap, !ty.subMap.isBottom)
        .app
    else app >> "Bot"

  /** completion record types */
  given compTyRule: Rule[CompTy] = (app, ty) =>
    given Rule[PureValueTy] = topRule(pureValueTyRule)
    FilterApp(app)
      .add(ty.normal, !ty.normal.isBottom, "Normal")
      .add(ty.abrupt, !ty.abrupt.isBottom, "Abrupt")
      .app

  /** list types */
  given listTyRule: Rule[ListTy] = (app, ty) =>
    ty.elem match
      case None => app
      case Some(elem) =>
        if (elem.isBottom) app >> "Nil"
        else if (elem.isTop) app >> "List"
        else app >> "List[" >> elem >> "]"

  // predefined types
  lazy val predTys: List[(PureValueTy, String)] = List(
    ESPureValueT -> "ESValue",
  )

  /** pure value types (non-completion record types) */
  given pureValueTyRule: Rule[PureValueTy] = (app, origTy) =>
    var ty: PureValueTy = origTy
    if (ty.isTop) app >> "PureValue"
    else
      predTys
        .foldLeft(FilterApp(app)) {
          case (app, (pred, name)) =>
            app.add({ ty --= pred; name }, pred <= ty)
        }
        .add(ty.clo.map(s => s"\"$s\""), !ty.clo.isBottom, "Clo")
        .add(ty.cont, !ty.cont.isBottom, "Cont")
        .add(ty.name, !ty.name.isBottom)
        .add(ty.record, !ty.record.isBottom)
        .add(ty.list, !ty.list.isBottom)
        .add("Symbol", !ty.symbol.isBottom)
        .add(ty.astValue, !ty.astValue.isBottom)
        .add(ty.nt.map(_.toString), !ty.nt.isBottom, "Nt")
        .add("CodeUnit", !ty.codeUnit.isBottom)
        .add(ty.const.map(s => s"~$s~"), !ty.const.isBottom, "Const")
        .add(ty.math, !ty.math.isBottom, "Math")
        .add(ty.number, !ty.number.isBottom, "Number")
        .add("BigInt", !ty.bigInt.isBottom)
        .add(ty.str.map(s => s"\"$s\""), !ty.str.isBottom, "String")
        .add(ty.bool, !ty.bool.isBottom)
        .add("Undefined", !ty.undef.isBottom)
        .add("Null", !ty.nullv.isBottom)
        .add("Absent", !ty.absent.isBottom)
        .app

  /** named record types */
  given nameTyRule: Rule[NameTy] = (app, ty) =>
    ty.set match
      case Inf => app
      case Fin(set) =>
        given Rule[Set[String]] = setRule("", OR, "")
        app >> set

  /** record types */
  given recordTyRule: Rule[RecordTy] = (app, ty) =>
    import RecordTy.*
    given Rule[(String, ValueTy)] = {
      case (app, (key, value)) =>
        app >> "[[" >> key >> "]]"
        if (!value.isTop) app >> ": " >> value
        else app
    }
    given Rule[List[(String, ValueTy)]] = iterableRule("{ ", ", ", " }")
    ty match
      case Top       => app >> "AnyRecord"
      case Elem(map) => app >> map.toList.sortBy(_._1)

  /** AST value types */
  given astValueTyRule: Rule[AstValueTy] = (app, ty) =>
    app >> "Ast"
    ty match
      case AstTopTy         => app
      case AstNameTy(names) => app >> names
      case AstSingleTy(x, i, j) =>
        app >> ":" >> x >> "[" >> i >> "," >> j >> "]"

  /** boolean types */
  given boolTyRule: Rule[BoolTy] = (app, ty) =>
    ty.set match
      case set if set.isEmpty   => app
      case set if set.size == 1 => app >> (if (set.head) "True" else "False")
      case _                    => app >> "Boolean"

  /** sub map types */
  given subMapTyRule: Rule[SubMapTy] = (app, ty) =>
    app >> "SubMap[" >> ty.key >> " |-> " >> ty.value >> "]"

  // rule for bounded set lattice
  private given bsetRule[T: Ordering](using Rule[T]): Rule[BSet[T]] =
    (app, set) =>
      given Rule[List[T]] = iterableRule("[", ", ", "]")
      set match
        case Inf      => app
        case Fin(set) => app >> set.toList.sorted

  // rule for string set
  private given setRule[T: Ordering](using Rule[T]): Rule[Set[T]] =
    setRule("[", ", ", "]")
  private def setRule[T: Ordering](
    pre: String,
    sep: String,
    post: String,
  )(using Rule[T]): Rule[Set[T]] = (app, set) =>
    given Rule[List[T]] = iterableRule(pre, sep, post)
    app >> set.toList.sorted

  // rule for option type for top
  private def topRule[T <: Lattice[T]](
    tRule: Rule[T],
    pre: String = "[",
    post: String = "]",
  ): Rule[T] = (app, t) =>
    given Rule[T] = tRule
    if (!t.isTop) app >> pre >> t >> post
    else app

  // appender with filtering
  private class FilterApp(val app: Appender) {
    private var first = true
    def add[T](
      t: => T,
      valid: Boolean,
      pre: String = "",
      post: String = "",
    )(using tRule: Rule[T]): this.type =
      if (valid)
        if (!first) app >> OR
        else first = false
        app >> pre >> t >> post
      this
  }

  // rule for number
  private given numberRule: Rule[Number] = (app, number) =>
    number match
      case Number(Double.PositiveInfinity) => app >> "+INF"
      case Number(Double.NegativeInfinity) => app >> "-INF"
      case Number(n) if n.isNaN            => app >> "NaN"
      case Number(n)                       => app >> n
  given Ordering[Number] = Ordering.by(_.n)

  // separator for type disjuction
  private val OR = " | "
}
