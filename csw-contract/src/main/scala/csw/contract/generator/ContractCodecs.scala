package csw.contract.generator

import io.bullet.borer.Encoder
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveEncoder

object ContractCodecs extends ContractCodecs
trait ContractCodecs {
  implicit lazy val endpointCodec: Encoder[Endpoint] = deriveEncoder
  implicit def modelCodec2: Encoder[ModelType[_]] = Encoder { (w, v) =>
    v.write(w)
  }

  implicit lazy val serviceCodec: Encoder[Service]   = deriveEncoder
  implicit lazy val servicesCodec: Encoder[Services] = deriveEncoder
  implicit lazy val contractCodec: Encoder[Contract] = deriveEncoder
}
