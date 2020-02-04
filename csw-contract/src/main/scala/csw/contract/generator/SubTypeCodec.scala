package csw.contract.generator

import io.bullet.borer.{Codec, Encoder}

object SubTypeCodec {
  def encoder[Base, Sub <: Base](codec: Codec[Base]): Encoder[Sub] = codec.encoder.asInstanceOf[Encoder[Sub]]
}
