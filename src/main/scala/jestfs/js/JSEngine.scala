package jestfs.js

import jestfs.LINE_SEP
import jestfs.util.BaseUtils.*
import jestfs.error.*
import java.io.*
import java.time.Duration.ZERO
import org.graalvm.polyglot.*
import scala.collection.mutable.{Map => MMap}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import sys.process._
import scala.language.postfixOps
import java.util.StringJoiner
import scala.util.Using

/** JavaScript Engine utilities */
object JSEngine {

  /** default commands */
  val defaultCmd = Map(
    "d8" -> "d8 --ignore-unhandled-promises -e",
    "node" -> "node --unhandled-rejections=none -e",
    "js" -> "js -e",
    "sm" -> "sm -e 'ignoreUnhandledRejections();' -e",
    "jsc" -> s"jsc -e|DYLD_FRAMEWORK_PATH:${sys.env.getOrElse("WEBKIT_HOME", "NO-DIRECTORY")}/WebkitBuild/Release",
  )

  /** default engine: (command, version) */
  lazy val defaultEngine: Option[(String, String)] = Try {
    // d8
    val cmd = defaultCmd("d8")
    s"$cmd ''".!!
    (cmd, runUsingBinary(cmd, "console.log(version());").get)
  }.recoverWith(e =>
    validityCheckerWarning
    // GraalVM
    if (useGraal)
      Using(Engine.create()) { engine =>
        ("GraalVM", engine.getVersion())
      }
    else
      Failure(e),
  ).recoverWith(_ =>
    Try {
      // js
      val cmd = defaultCmd("js")
      s"$cmd ''".!!
      (cmd, runUsingBinary("js", "--version").get)
    },
  ).toOption

  lazy val defaultEngineToString = defaultEngine
    .map {
      case (e, v) => s"Engine: $e${LINE_SEP}Version: $v"
    }
    .getOrElse("NO-DEFAULT-ENGINE")

  /** warn user if d8 can't be used */
  private lazy val validityCheckerWarning = warn(
    "Could not use d8. Validity checker might pass the invalid program.",
  )

  /** Check if Graal can be used in this environment */
  lazy val useGraal: Boolean =
    try
      Using(
        Context
          .newBuilder("js")
          .option("engine.WarnInterpreterOnly", "false")
          .build(),
      ) { context =>
        if (context.getEngine.getImplementationName == "Interpreted") then
          warn("Graal is running on interpreted mode.")
          throw Error()
        else
          try {
            context.eval("js", "")
          } catch
            case e => {
              warn("Unable to run js using Graal. try `gu --jvm install js`.")
              throw e
            }
      }.isSuccess
    catch {
      case e: Error =>
        warn("Unable to run Graal.")
        false
    }

  /** run a javascript code with default engine */
  def run(src: String, timeout: Option[Int] = None): Try[String] = defaultEngine
    .map {
      case ("GraalVM", _) => runUsingGraal(src, timeout)
      case (e, _)         => runUsingBinary(e, src)
    }
    .getOrElse(
      error("Default engine is not set"),
    )

  // -------------------------------------------------------------------------
  // runners in temporal context
  // -------------------------------------------------------------------------

  /** Exception that indicates given JS code throws exception */
  class JSException(message: String) extends Exception(message)

  /** resolve PolyglotException to other exceptions */
  def polyglotExceptionResolver[T](e: Throwable): Try[T] = e match {
    case e: PolyglotException if (e.isInterrupted || e.isCancelled) =>
      Failure(TimeoutException("JSEngine timeout"))
    case e: PolyglotException if e.isGuestException =>
      Failure(JSException(e.getMessage))
    case e: PolyglotException if e.isHostException =>
      Failure(e.asHostException)
    case _ =>
      Failure(e)
  }

  case class Status(var running: Boolean = false, var done: Boolean = false)

  /** register timeout to context in milliseconds */
  def registerTimeout(context: Context, timeout: Int, stat: Status) =
    Future {
      while (!stat.done) {
        Thread.sleep(timeout)
        if (!stat.done && stat.running)
          // TODO race condition:
          // new eval is performed between
          // (!stat.done) check and interrupt
          context.interrupt(ZERO)
      }
    }

  /** execute a javascript program */
  def runUsingContext(
    src: String,
    context: Context,
    timeout: Option[Int] = None,
  ): Unit =
    if (!useGraal) throw NoGraalError
    val stat = Status()
    timeout.foreach(millis => registerTimeout(context, millis, stat))
    stat.running = true
    try context.eval("js", src)
    finally stat.done = true

  /** execute a javascript program, and gets its stdout */
  def runAndGetStdout(
    src: String,
    context: Context,
    out: ByteArrayOutputStream,
    timeout: Option[Int] = None,
  ): String =
    if (!useGraal) throw NoGraalError
    val stat = Status()
    out.reset
    timeout.foreach(millis => registerTimeout(context, millis, stat))
    stat.running = true
    try {
      context.eval("js", src)
      out.toString
    } finally stat.done = true

  def usingContext[T](f: (Context, ByteArrayOutputStream) => T): Try[T] =
    if (!useGraal) throw NoGraalError
    val out = new ByteArrayOutputStream()
    Using(Context.newBuilder("js").out(out).build()) { context =>
      f(context, out)
    }.recoverWith(e => polyglotExceptionResolver(e))

  def runUsingGraal(src: String, timeout: Option[Int] = None): Try[String] =
    usingContext((context, out) => runAndGetStdout(src, context, out, timeout))

  // -------------------------------------------------------------------------
  // runners in saved context
  // -------------------------------------------------------------------------

  /** Saved contexts */
  val contextMap: MMap[String, (Context, ByteArrayOutputStream)] = MMap()

  def getContext(id: String): (Context, ByteArrayOutputStream) =
    contextMap.getOrElseUpdate(
      id, {
        val out = new ByteArrayOutputStream()
        (Context.newBuilder("js").out(out).build(), out)
      },
    )

  /** execute a javascript program with the stored context */
  def runInContext(id: String, src: String): Try[Value] =
    if (!useGraal) throw NoGraalError

    val (context, out) = getContext(id)

    Try(context.eval("js", src))

  /** execute a javascript program with stored context, and gets its stdout */
  def runInContextAndGetStdout(id: String, src: String): Try[String] =
    if (!useGraal) throw NoGraalError

    val (context, out) = getContext(id)

    Try(context.eval("js", src)).map(_ => {
      val result = out.toString
      out.flush()
      result
    })

  // -------------------------------------------------------------------------
  // runners by executing shell command with path to binary
  // -------------------------------------------------------------------------
  def runUsingBinary(runner: String, src: String): Try[String] = Try {
    val escapedSrc = escape(src)
    val stdout = new StringJoiner(LINE_SEP)
    val stderr = new StringJoiner(LINE_SEP)
    def cmd(main: String) = s"timeout -k 10s 3s $main $escapedSrc"
    val pb: ProcessBuilder = if runner.contains("|") then {
      val Array(main, envInfo) = runner.split("\\|")
      val Array(envKey, envVal) = envInfo.split(":")
      Process(cmd(main), None, envKey -> envVal)
    } else cmd(runner)

    pb ! ProcessLogger(
      out => stdout.add(out),
      err => stderr.add(err),
    ) match {
      case 0   => stdout.toString
      case 124 | 137 => throw TimeoutException(runner)
      case 127 => throw NoCommandError(runner)
      case st  => throw new Exception(stdout.toString + stderr.toString),
    }
  }

  def runUsingD8(src: String): Try[String] =
    runUsingBinary(defaultCmd("d8"), src)

  def runUsingJs(src: String): Try[String] =
    runUsingBinary(defaultCmd("js"), src)

  /** escape a string to a shell-safe string, enclosed by single quote */
  private def escape(string: String): String =
    val replaced = string.replace("'", "'\"'\"'") // replace I'm to I'"'"'m
    s"'$replaced'"
}
