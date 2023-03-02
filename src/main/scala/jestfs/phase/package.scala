package jestfs.phase

import scala.util.matching.Regex
import jestfs.util.OptionKind

/** argument regular expressions */
type ArgRegex[T] = (Regex, Regex, (T, String) => Unit)

/** phase options */
type PhaseOption[T] = (String, OptionKind[T], String)
