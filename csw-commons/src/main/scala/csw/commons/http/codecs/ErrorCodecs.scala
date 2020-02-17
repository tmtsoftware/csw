package csw.commons.http.codecs

import csw.commons.http.{ErrorMessage, ErrorResponse}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec

object ErrorCodecs extends ErrorCodecs
trait ErrorCodecs {
  implicit lazy val errorMessageCodec: Codec[ErrorMessage]   = deriveCodec
  implicit lazy val errorResponseCodec: Codec[ErrorResponse] = deriveCodec
}
