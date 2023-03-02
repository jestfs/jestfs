package jestfs.error

export java.util.concurrent.TimeoutException
object TimeoutException {
  def apply(msg: String) = java.util.concurrent.TimeoutException(msg)
}
