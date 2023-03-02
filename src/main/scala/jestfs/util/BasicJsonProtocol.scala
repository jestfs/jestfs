package jestfs.util

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*

/** basic JSON protocols */
trait BasicJsonProtocol {
  // decoder for set
  given setDecoder[T](using
    tDecoder: Decoder[T],
  ): Decoder[Set[T]] = new Decoder {
    final def apply(c: HCursor): Decoder.Result[Set[T]] = (for {
      vector <- c.value.asArray
    } yield vector.foldLeft[Decoder.Result[Set[T]]](Right(Set[T]())) {
      case (prev, json) =>
        for {
          set <- prev
          t <- tDecoder(json.hcursor)
        } yield set + t
    }).getOrElse(unknownFail("set", c))
  }

  // encoder for set
  given setEncoder[T](using
    tEncoder: Encoder[T],
  ): Encoder[Set[T]] = new Encoder {
    final def apply(set: Set[T]): Json =
      Json.fromValues(set.map(tEncoder.apply))
  }

  // decoder for option
  given optionDecoder[T](using
    tDecoder: Decoder[T],
  ): Decoder[Option[T]] = new Decoder {
    final def apply(c: HCursor): Decoder.Result[Option[T]] =
      if (c.value.isNull) Right(None) else tDecoder(c).map(Some(_))
  }

  // encoder for option
  given optionEncoder[T](using
    tEncoder: Encoder[T],
  ): Encoder[Option[T]] = new Encoder {
    final def apply(opt: Option[T]): Json =
      opt.fold(Json.Null)(tEncoder.apply)
  }

  // decoder for pair
  given pairDecoder[A, B](using
    aDecoder: Decoder[A],
    bDecoder: Decoder[B],
  ): Decoder[(A, B)] = new Decoder {
    final def apply(c: HCursor): Decoder.Result[(A, B)] = (for {
      Vector(x, y) <- c.value.asArray
    } yield for {
      a <- aDecoder(x.hcursor)
      b <- bDecoder(y.hcursor)
    } yield (a, b)).getOrElse(unknownFail("pair", c))
  }

  // encoder for pair
  given pairEncoder[A, B](using
    aEncoder: Encoder[A],
    bEncoder: Encoder[B],
  ): Encoder[(A, B)] = new Encoder {
    final def apply(pair: (A, B)): Json =
      val (a, b) = pair
      Json.fromValues(Seq(aEncoder(a), bEncoder(b)))
  }

  // decoder for IntId: id -> IntId
  def idDecoder[T <: IntId](getter: Int => Option[T]): Decoder[T] =
    new Decoder[T] {
      final def apply(c: HCursor): Decoder.Result[T] = (for {
        number <- c.value.asNumber
        id <- number.toInt
        x <- getter(id)
      } yield Right(x)).getOrElse(unknownFail("id", c))
    }

  // encoder for IntId: IntId -> id
  def idEncoder[T <: IntId]: Encoder[T] = new Encoder[T] {
    final def apply(x: T): Json = Json.fromInt(x.id)
  }

  // decoder for IntId with name: { name: id } -> IntId
  def idDecoder[T <: IntId](
    name: String,
    getter: Int => Option[T],
  ): Decoder[T] = new Decoder[T] {
    final def apply(c: HCursor): Decoder.Result[T] = (for {
      obj <- c.value.asObject
      value <- obj(name)
      number <- value.asNumber
      id <- number.toInt
      x <- getter(id)
    } yield Right(x)).getOrElse(unknownFail("id", c))
  }

  // encoder for IntId with name: IntId -> { name: id }
  def idEncoder[T <: IntId](name: String): Encoder[T] = new Encoder[T] {
    final def apply(x: T): Json =
      Json.fromFields(Seq(name -> Json.fromInt(x.id)))
  }

  // decoding failure
  def decodeFail[T](msg: String, c: HCursor): Decoder.Result[T] =
    Left(DecodingFailure(msg, c.history))

  // decoding failure
  def unknownFail[T](name: String, c: HCursor): Decoder.Result[T] =
    decodeFail(s"unknown $name: ${c.value}", c)
}
