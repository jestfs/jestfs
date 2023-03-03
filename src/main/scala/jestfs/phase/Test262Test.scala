package jestfs.phase

import jestfs.*
import jestfs.cfg.CFG
import jestfs.error.*
import jestfs.interpreter.*
import jestfs.state.*
import jestfs.util.*
import jestfs.util.BaseUtils.*
import jestfs.util.SystemUtils.*
import jestfs.es.*
import jestfs.es.util.{Coverage, withCFG}
import jestfs.test262.{*, given}
import jestfs.test262.util.TestFilter
import java.io.File

/** `test262-test` phase */
case object Test262Test extends Phase[CFG, Summary] {
  val name = "test262-test"
  val help = "tests Test262 tests with harness files (default: tests/test262)."
  def apply(
    cfg: CFG,
    cmdConfig: CommandConfig,
    config: Config,
  ): Summary = withCFG(cfg) {
    // set test mode
    if (!config.noTestMode) TEST_MODE = true

    // get target version of Test262
    val version = Test262.getVersion(config.target)
    val test262 = Test262(version)

    // run test262 eval test in debugging mode
    if (config.debug)
      test262.evalTest(
        cmdConfig.targets,
        kFs = config.kFs,
        cp = config.cp,
      )
    // run test262 eval test
    else
      test262.evalTest(
        cmdConfig.targets,
        config.log,
        config.progress,
        config.coverage,
        config.timeLimit,
        config.kFs,
        config.cp,
      )
  }

  def defaultConfig: Config = Config()
  val options: List[PhaseOption[Config]] = List(
    (
      "debug",
      BoolOption(c => c.debug = true),
      "turn on the debugging mode.",
    ),
    (
      "log",
      BoolOption(c => c.log = true),
      "turn on logging mode.",
    ),
    (
      "target",
      StrOption((c, s) => c.target = Some(s)),
      "set the target git version of Test262 (default: current version).",
    ),
    (
      "progress",
      BoolOption(c => c.progress = true),
      "show progress bar.",
    ),
    (
      "coverage",
      BoolOption(c => c.coverage = true),
      "measure node/branch coverage in CFG of ECMA-262.",
    ),
    (
      "timeout",
      NumOption((c, k) => c.timeLimit = Some(k)),
      "set the time limit in seconds (default: no limit).",
    ),
    (
      "k-fs",
      NumOption((c, k) => c.kFs = k),
      "set the k-value for feature sensitivity. (default: 0)",
    ),
    (
      "cp",
      BoolOption(c => c.cp = true),
      "turn on the call-path mode (default: false) (meaningful if k-fs > 0).",
    ),
    (
      "no-test-mode",
      BoolOption(c => c.noTestMode = true),
      "set no test mode for print.",
    ),
  )
  case class Config(
    var target: Option[String] = None,
    var debug: Boolean = false,
    var log: Boolean = false,
    var coverage: Boolean = false,
    var progress: Boolean = false,
    var timeLimit: Option[Int] = None,
    var kFs: Int = 0,
    var cp: Boolean = false,
    var noTestMode: Boolean = false,
  )
}
