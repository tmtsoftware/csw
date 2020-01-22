package csw.contract.generator.codecs

import csw.contract.generator.models._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec

object ContractCodecs extends ContractCodecs
trait ContractCodecs {
  implicit lazy val endpointCodec: Codec[Endpoint] = deriveCodec
  implicit lazy val modelCodec: Codec[ModelAdt]    = deriveCodec

  implicit lazy val serviceCodec: Codec[Service]   = deriveCodec
  implicit lazy val servicesCodec: Codec[Services] = deriveCodec
}
