package csw.params.core.formats

import csw.params.core.models.Prefix
import io.bullet.borer.Codec

trait CborCommonSupport {
  implicit lazy val prefixCodec: Codec[Prefix] = Codec.forCaseClass[Prefix]
}
