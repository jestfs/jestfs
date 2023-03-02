package jestfs.es.util.injector

import jestfs.error.*
import jestfs.state.*
import jestfs.util.BaseUtils.*

/** exit status tag */
trait ExitTag:
  override def toString: String = this match
    case NormalTag                   => s"normal"
    case TimeoutTag                  => s"timeout"
    case SpecErrorTag(error, cursor) => s"spec-error: $cursor"
    case TranspileFailTag            => s"transpile-failure"
    case ThrowValueTag(value: Value) => s"throw-value: $value"
    case ThrowErrorTag(errorName, msg) =>
      s"throw-error: ${errorName}${msg.map(msg => s"($msg)").getOrElse("")}"
  def equivalent(that: ExitTag): Boolean = (this, that) match
    case (_: ThrowValueTag, _: ThrowValueTag)               => true
    case (ThrowErrorTag(name1, _), ThrowErrorTag(name2, _)) => name1 == name2
    case _                                                  => this == that
object ExitTag:
  /** Get exit tag from exit status */
  def apply(st: => State): ExitTag = try {
    st(GLOBAL_RESULT) match
      case Undef => NormalTag
      case comp @ Comp(CONST_THROW, addr: DynamicAddr, _) =>
        st(addr)(Str("Prototype")) match
          case NamedAddr(errorNameRegex(errorName)) =>
            ThrowErrorTag(errorName, st.heap.map(addr)._2.map(_.toString))
          case _ => ThrowValueTag(addr)
      case comp @ Comp(CONST_THROW, value, _) => ThrowValueTag(value)
      case v => error(s"unexpected exit status: $v")
  } catch {
    case _: TimeoutException   => TimeoutTag
    case e: InterpreterErrorAt => SpecErrorTag(e.error, e.cursor)
  }

  /** Get exit tag by parsing */
  def apply(tag: => String): Option[ExitTag] = optional {
    val specErrorPattern = "spec-error: .*".r
    val throwValuePattern = "throw-value: .*".r
    val throwErrorPattern = "throw-error: (\\w+).*".r
    tag match {
      case "normal"                => NormalTag
      case "timeout"               => TimeoutTag
      case specErrorPattern()      => ???
      case "transpile-failure"     => TranspileFailTag
      case throwValuePattern()     => ThrowValueTag(Str(""))
      case throwErrorPattern(name) => ThrowErrorTag(name)
      case _                       => ???
    }
  }

  /** error name regex pattern */
  lazy val errorNameRegex = "INTRINSICS.([A-Z][a-z]+Error).prototype".r

/** normal exit */
case object NormalTag extends ExitTag

/** timeout */
case object TimeoutTag extends ExitTag

/** an error is thrown in specification */
case class SpecErrorTag(error: JestFsError, cursor: Cursor) extends ExitTag

/** an error is thrown during transpilation */
case object TranspileFailTag extends ExitTag

/** an error is thrown with a ECMAScript value */
case class ThrowValueTag(value: Value) extends ExitTag

/** an error is thrown with an ECMAScript error */
case class ThrowErrorTag(
  errorName: String,
  msg: Option[String] = None,
) extends ExitTag
