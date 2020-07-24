package csw.contract.generator

import io.bullet.borer.{Codec, Decoder, Encoder, Target}

object RoundTrip {
  def roundTrip(modelData: Any, codec: Codec[_], format: Target): Any = {
    val bytes = format.encode(modelData)(codec.encoder.asInstanceOf[Encoder[Any]]).toByteArray
    format.decode(bytes).to[Any](codec.decoder.asInstanceOf[Decoder[Any]]).value
  }
}
