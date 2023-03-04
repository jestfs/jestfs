package jestfs

import jestfs.error.*
import jestfs.phase.*
import jestfs.util.*

/** JestFs top-level object */
object JestFs extends Git(BASE_DIR) {

  /** the main entry point of JestFs. */
  def main(tokens: Array[String]): Unit = try
    tokens.toList match
      case Nil                                        => println(welcome)
      case List("--version" | "-version" | "version") => println(VERSION)
      case str :: args =>
        cmdMap.get(str) match {
          case Some(cmd) => cmd(args)
          case None      => throw NoCmdError(str)
        }
  catch
    // JestFsError: print only the error message.
    case e: JestFsError =>
      Console.err.println(e.getMessage)
      if (ERROR_MODE) throw e
      if (STATUS_MODE) sys.exit(1)
    // Unexpected: print the stack trace.
    case e: Throwable =>
      Console.err.println(s"[JestFs v$VERSION] Unexpected error occurred:")
      throw e

  /** execute JestFs with a runner */
  def apply[Result](
    command: Command[Result],
    runner: CommandConfig => Result,
    config: CommandConfig,
  ): Result =
    // silent for help command
    if (command == CmdHelp) config.silent = true
    // target existence check
    if (command.needTarget && config.targets.isEmpty)
      throw NoTargetError(command)
    // set the start time.
    val startTime = System.currentTimeMillis
    // execute the command.
    val result: Result = runner(config)
    // duration
    val duration = Time(System.currentTimeMillis - startTime)
    // display the result.
    if (!config.silent) command.showResult(result)
    // display the time.
    if (config.time)
      val name = config.command.name
      println(f"The command '$name' took $duration.")
    // return result
    result

  /** welcome message */
  val welcome: String =
    s"""Welcome to `jestfs` - JavaScript conformance test generator using feature-sensitive coverage.
       |Please type `jestfs help` to see the help message.""".stripMargin

  /** commands */
  val commands: List[Command[_]] = List(
    CmdHelp,
    CmdFuzz,
    CmdConformTest,
    CmdTest262Test,
    CmdCategorizeBug,
    CmdDrawFigure,
  )
  val cmdMap = commands.foldLeft[Map[String, Command[_]]](Map()) {
    case (map, cmd) => map + (cmd.name -> cmd)
  }

  /** phases */
  var phases: List[Phase[_, _]] = List(
    Help,
    // Mechanized Specification Extraction
    Extract,
    Compile,
    BuildCFG,
    // Conformance Test Generation
    Fuzz,
    GenTest,
    ConformTest,
    // Test262
    Test262Test,
    // Evaluation
    CategorizeBug,
    DrawFigure,
  )

  /** command options */
  val options: List[PhaseOption[CommandConfig]] = List(
    ("silent", BoolOption(c => c.silent = true), "do not show final results."),
    ("error", BoolOption(_ => ERROR_MODE = true), "show error stack traces."),
    ("status", BoolOption(_ => STATUS_MODE = true), "exit with status."),
    ("time", BoolOption(c => c.time = true), "display the duration time."),
  )
}

/** command configuration */
case class CommandConfig(
  var command: Command[_],
  var targets: List[String] = Nil,
  var silent: Boolean = false,
  var time: Boolean = false,
)
