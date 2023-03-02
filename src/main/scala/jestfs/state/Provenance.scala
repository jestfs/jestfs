package jestfs.state

import jestfs.cfg.*

/** provenance of addresses */
case class Provenance(
  cursor: Cursor,
  feature: Option[Feature],
) extends StateElem
