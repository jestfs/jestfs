package jestfs.es.builtin

import jestfs.state.*

/** builtin model structure */
case class Struct(
  typeName: String,
  imap: List[(String, PureValue)] = List(),
  nmap: List[(String, Property)] = List(),
)
