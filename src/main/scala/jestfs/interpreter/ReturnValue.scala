package jestfs.interpreter

import jestfs.state.Value

/** special class for handle return */
case class ReturnValue(value: Value) extends Throwable
