package jestfs.error

// not supported errors
case class NotSupported(msg: String) extends JestFsError(msg, "NotSupported")
