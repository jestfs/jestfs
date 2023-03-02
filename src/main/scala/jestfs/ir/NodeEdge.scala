package jestfs.ir

import jestfs.cfg.Node

/** edge to enclosing CFG nodes */
trait NodeEdge:
  /** edge to enclosing CFG nodes */
  var cfgNode: Option[Node] = None
