package jestfs.state

import jestfs.cfg.Call

/** abstraction of call stack as graph */
case class CallGraph(
  start: Call,
  edges: Set[(Call, Call)],
  end: Call,
) extends StateElem {
  def +(call: Call): CallGraph = CallGraph(start, edges + (end -> call), call)
  def +(that: CallGraph): CallGraph =
    CallGraph(
      this.start,
      this.edges ++ that.edges + (this.end -> that.start),
      that.end,
    )
}
object CallGraph {
  def apply(call: Call): CallGraph = CallGraph(call, Set(), call)
}
