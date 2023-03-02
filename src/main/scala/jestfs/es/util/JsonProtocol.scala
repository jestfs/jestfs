package jestfs.es.util

import jestfs.ir.EReturnIfAbrupt
import jestfs.cfg.*
import jestfs.state.util.{JsonProtocol => StateJsonProtocol}
import jestfs.util.*
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*

/** JSON protocols for CFG */
class JsonProtocol(cfg: CFG) extends StateJsonProtocol(cfg) {
  import Coverage.*

  // branch or reference to EReturnIfAbrupt with boolean values
  given condDecoder: Decoder[Cond] = deriveDecoder
  given condEncoder: Encoder[Cond] = deriveEncoder
  given condElemDecoder: Decoder[Branch | WeakUIdRef[EReturnIfAbrupt]] =
    val branchDecoder = idDecoder[Branch | WeakUIdRef[EReturnIfAbrupt]](
      "branch",
      cfg.nodeMap.get(_).collect { case branch: Branch => branch },
    )
    val abruptDecoder = idDecoder[Branch | WeakUIdRef[EReturnIfAbrupt]](
      "abrupt",
      cfg.riaExmprMap.get(_).map(_.idRef),
    )
    branchDecoder.handleErrorWith(_ => abruptDecoder)
  given condElemEncoder: Encoder[Branch | WeakUIdRef[EReturnIfAbrupt]] =
    new Encoder {
      final def apply(x: Branch | WeakUIdRef[EReturnIfAbrupt]): Json = x match
        case branch: Branch                      => idEncoder("branch")(branch)
        case abrupt: WeakUIdRef[EReturnIfAbrupt] => idEncoder("abrupt")(abrupt)
    }

  // syntax-sensitive views
  given viewDecoder: Decoder[View] = optionDecoder
  given viewEncoder: Encoder[View] = optionEncoder
  given nodeViewDecoder: Decoder[NodeView] = deriveDecoder
  given nodeViewEncoder: Encoder[NodeView] = deriveEncoder
  given condViewDecoder: Decoder[CondView] = deriveDecoder
  given condViewEncoder: Encoder[CondView] = deriveEncoder
  given funcViewDecoder: Decoder[FuncView] = deriveDecoder
  given funcViewEncoder: Encoder[FuncView] = deriveEncoder

  // meta-info for each view or features
  given nodeViewInfoDecoder: Decoder[NodeViewInfo] = deriveDecoder
  given nodeViewInfoEncoder: Encoder[NodeViewInfo] = deriveEncoder
  given condViewInfoDecoder: Decoder[CondViewInfo] = deriveDecoder
  given condViewInfoEncoder: Encoder[CondViewInfo] = deriveEncoder

  // Coverage class
  given coverageConstructorDecoder: Decoder[CoverageConstructor] = deriveDecoder
  given coverageConstructorEncoder: Encoder[CoverageConstructor] = deriveEncoder
}
