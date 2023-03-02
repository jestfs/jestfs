package jestfs.phase

import jestfs.*
import jestfs.cfg.CFG
import jestfs.cfgBuilder.CFGBuilder
import jestfs.ir.Program
import jestfs.util.*
import jestfs.util.BaseUtils.*
import jestfs.util.SystemUtils.*

/** `build-cfg` phase */
case object BuildCFG extends Phase[Program, CFG] {
  val name = "build-cfg"
  val help = "builds a control-flow graph (CFG) from an IR program."
  def apply(
    program: Program,
    cmdConfig: CommandConfig,
    config: Config,
  ): CFG = {
    // build cfg
    val builder = new CFGBuilder(program, config.log)
    val cfg = builder.result

    // logging mode
    if (config.log) cfg.dumpTo(CFG_LOG_DIR)

    // print DOT files
    if (config.dot)
      val dotDir = s"$CFG_LOG_DIR/dot"
      if (config.pdf && !isNormalExit("dot -V"))
        warn("- Graphviz `dot` is not installed!")
        warn("  (https://graphviz.org/doc/info/lang.html)")
        warn("- The `-build-cfg:pdf` option is turned off.")
        config.pdf = false
      cfg.dumpDot(dotDir, config.pdf)

    cfg
  }
  def defaultConfig: Config = Config()
  val options: List[PhaseOption[Config]] = List(
    (
      "log",
      BoolOption(c => c.log = true),
      "turn on logging mode.",
    ),
    (
      "dot",
      BoolOption(c => c.dot = true),
      "dump the cfg in a DOT format.",
    ),
    (
      "pdf",
      BoolOption(c => { c.dot = true; c.pdf = true }),
      "dump the cfg in DOT and PDF formats.",
    ),
  )
  case class Config(
    var log: Boolean = false,
    var dot: Boolean = false,
    var pdf: Boolean = false,
  )
}
