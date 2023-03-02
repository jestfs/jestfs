package jestfs.state.util

import jestfs.cfg.*
import jestfs.cfg.util.{JsonProtocol => CFGJsonProtocol}
import jestfs.ir.EReturnIfAbrupt
import jestfs.spec.{SyntaxDirectedOperationHead, BuiltinHead}
import jestfs.state.*
import jestfs.util.*
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*

/** JSON protocols for states */
class JsonProtocol(cfg: CFG) extends CFGJsonProtocol(cfg) {
  // abstraction of call stack as graph
  given callGraphDecoder: Decoder[CallGraph] = deriveDecoder
  given callGraphEncoder: Encoder[CallGraph] = deriveEncoder

  // abstraction of call stacks as simple paths
  given callPathDecoder: Decoder[CallPath] = new Decoder {
    final def apply(c: HCursor): Decoder.Result[CallPath] =
      c.value.as[List[Call]].map(calls => CallPath(calls, calls.toSet))
  }
  given callPathEncoder: Encoder[CallPath] = new Encoder {
    final def apply(callPath: CallPath): Json =
      Json.fromValues(callPath.path.map(_.asJson))
  }

  // ECMAScript features
  given featureDecoder: Decoder[Feature] = new Decoder {
    final def apply(c: HCursor): Decoder.Result[Feature] = for {
      func <- funcDecoder(c)
      feature <- func.head match
        case Some(head: SyntaxDirectedOperationHead) =>
          Right(SyntacticFeature(func, head))
        case Some(head: BuiltinHead) =>
          Right(BuiltinFeature(func, head))
        case _ =>
          unknownFail("feature", c)
    } yield feature
  }
  given featureEncoder: Encoder[Feature] = new Encoder {
    final def apply(feature: Feature): Json = funcEncoder(feature.func)
  }
}
