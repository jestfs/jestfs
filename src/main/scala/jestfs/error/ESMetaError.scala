package jestfs.error

import jestfs.VERSION

class JestFsError(
  val errMsg: String,
  val tag: String = s"JestFs v$VERSION",
) extends Error(s"[$tag] $errMsg")
