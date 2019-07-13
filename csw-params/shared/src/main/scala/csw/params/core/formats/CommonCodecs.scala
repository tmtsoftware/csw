package csw.params.core.formats

import csw.params.core.models.Prefix
import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodecForUnaryCaseClass

trait CommonCodecs {
  implicit lazy val prefixCodec: Codec[Prefix] = deriveCodecForUnaryCaseClass[Prefix]
}
