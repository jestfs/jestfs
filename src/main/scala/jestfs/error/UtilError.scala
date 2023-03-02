package jestfs.error

sealed abstract class UtilError(msg: String)
  extends JestFsError(msg, "UtilError")

case class WrongUId(name: String, uid: Int)
  extends UtilError(s"[WrongUId] uid $uid does not exist in $name.")

case object NoGraalError extends UtilError("no Graal engine")

case object TranspileFailureError extends UtilError("Trnaspile failure")

case class NoCommandError(command: String)
  extends UtilError(s"Command not found: $command")
