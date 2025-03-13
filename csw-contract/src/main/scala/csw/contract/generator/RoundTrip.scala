/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract.generator

import io.bullet.borer.{Codec, Decoder, Encoder, Target}

object RoundTrip {
  def roundTrip(modelData: Any, codec: Codec[?], format: Target): Any = {
    val bytes = format.encode(modelData)(using codec.encoder.asInstanceOf[Encoder[Any]]).toByteArray
    format.decode(bytes).to[Any](using codec.decoder.asInstanceOf[Decoder[Any]]).value
  }
}
