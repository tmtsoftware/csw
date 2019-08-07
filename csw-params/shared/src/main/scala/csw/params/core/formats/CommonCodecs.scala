package csw.params.core.formats

import csw.params.core.models.Prefix
import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryCodec

trait CommonCodecs {
  implicit lazy val prefixCodec: Codec[Prefix] = deriveUnaryCodec[Prefix]
}
