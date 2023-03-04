package jestfs

/** line seperator */
val LINE_SEP = System.getProperty("line.separator")

/** base project directory root */
val BASE_DIR = System.getenv("JESTFS_HOME")

/** base project directory root */
val VERSION = "0.1.0"

/** log directory */
val LOG_DIR = s"$BASE_DIR/logs"
val EXTRACT_LOG_DIR = s"$LOG_DIR/extract"
val COMPILE_LOG_DIR = s"$LOG_DIR/compile"
val CFG_LOG_DIR = s"$LOG_DIR/cfg"
val FUZZ_LOG_DIR = s"$LOG_DIR/fuzz"
val INJECT_LOG_DIR = s"$LOG_DIR/inject"
val GENTEST_LOG_DIR = s"$LOG_DIR/gen-test"
val CONFORMTEST_LOG_DIR = s"$LOG_DIR/conform-test"
val LOCALIZE_LOG_DIR = s"$LOG_DIR/localize"
val CATEGORIZE_LOG_DIR = s"$LOG_DIR/categorize"
val DRAW_FIGURE_LOG_DIR = s"$LOG_DIR/draw-figure"
val EVAL_LOG_DIR = s"$LOG_DIR/eval"
val TEST262TEST_LOG_DIR = s"$LOG_DIR/test262"

/** specification directory */
val ECMA262_DIR = s"$BASE_DIR/ecma262"
val SPEC_HTML = s"$ECMA262_DIR/spec.html"

/** current directory root */
val CUR_DIR = System.getProperty("user.dir")

/** source code directory */
val SRC_DIR = s"$BASE_DIR/src/main/scala/jestfs"

/** resource directory */
val RESOURCE_DIR = s"$BASE_DIR/src/main/resources"
val UNICODE_DIR = s"$RESOURCE_DIR/unicode"
val MANUALS_DIR = s"$RESOURCE_DIR/manuals"
val RESULT_DIR = s"$RESOURCE_DIR/result"

/** package name */
val PACKAGE_NAME = "jestfs"

/** tests directory */
val TEST262_DIR = s"$BASE_DIR/test262"
val TEST262_TEST_DIR = s"$TEST262_DIR/test"

/** error stack trace display mode */
var ERROR_MODE = false

/** exit status return mode */
var STATUS_MODE = false

// -----------------------------------------------------------------------------
// Mutable Global Options
// -----------------------------------------------------------------------------
/** test mode (turn it on only when executing tests) */
var TEST_MODE: Boolean = false
