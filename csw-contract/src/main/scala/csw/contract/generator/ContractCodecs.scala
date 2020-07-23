package csw.contract.generator

import io.bullet.borer.{Decoder, Encoder}
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveEncoder

object ContractCodecs extends ContractCodecs
trait ContractCodecs {
  implicit lazy val endpointEncoder: Encoder[Endpoint]      = deriveEncoder
  implicit lazy val modelTypeEncoder: Encoder[ModelType[_]] = Encoder((w, v) => v.write(w))

  implicit lazy val modelTypeDecoder: Decoder[ModelType[_]] = Decoder { r =>
    r.tryReadMapHeader(1) || r.tryReadMapStart() || r.tryReadArrayHeader(1) || r.tryReadArrayStart()
    val modeTypeName = r.readString()
    val dec          = ModelSet.modeTypeDecoder(modeTypeName)
    dec.read(r)
  }

  implicit lazy val modelSetEncoder: Encoder[ModelSet] = Encoder[Map[String, ModelType[_]]]
    .contramap(_.modelTypes.map(x => x.name -> x).toMap)

  implicit lazy val readmeEncoder: Encoder[Readme]     = deriveEncoder
  implicit lazy val contractEncoder: Encoder[Contract] = deriveEncoder
  implicit lazy val serviceEncoder: Encoder[Service]   = deriveEncoder
  implicit lazy val servicesEncoder: Encoder[Services] = deriveEncoder
}
