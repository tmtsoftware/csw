package csw.config.server.http.tapir

import io.bullet.borer._
import sttp.tapir.Codec._
import sttp.tapir.DecodeResult.{Error, Value}
import sttp.tapir._

object TapirJsonBorer extends TapirJsonBorer

trait TapirJsonBorer {
  def jsonBody[T: Encoder: Decoder: Schema: Validator]: EndpointIO.Body[String, T] = anyFromUtf8StringBody(circeCodec[T])

  implicit def circeCodec[T: Encoder: Decoder: Schema: Validator]: JsonCodec[T] =
    sttp.tapir.Codec.json { s =>
      Json.decode(s.getBytes).to[T].valueEither match {
        case Left(error) => Error(s, error)
        case Right(v)    => Value(v)
      }
    } { t => Json.encode(t).toUtf8String }

}
