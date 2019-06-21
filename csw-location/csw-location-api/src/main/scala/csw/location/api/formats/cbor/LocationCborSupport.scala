package csw.location.api.formats.cbor

import akka.Done
import csw.location.api.models.{AkkaLocation, Registration}
import csw.params.core.formats.CborHelpers
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._

trait LocationCborSupport extends ActorSystemDependentCborSupport {
  implicit val akkaLocationCodec: Codec[AkkaLocation] = deriveCodec[AkkaLocation]
  implicit val registrationCodec: Codec[Registration] = deriveCodec[Registration]
  implicit val doneCodec: Codec[Done]                 = CborHelpers.bimap[String, Done](_ => Done, _ => "done")
}
