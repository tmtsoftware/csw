package csw.services.location.internal

import akka.Done
import csw.messages.extensions.Formats
import csw.messages.extensions.Formats.MappableFormat
import csw.messages.location.{ActorSystemDependentFormats, AkkaLocation}
import csw.services.location.api.models.Registration
import julienrf.json.derived
import play.api.libs.json._

trait LocationJsonSupport extends ActorSystemDependentFormats {
  implicit val akkaLocationFormat: Format[AkkaLocation]  = Json.format[AkkaLocation]
  implicit val registrationFormat: OFormat[Registration] = derived.flat.oformat((__ \ "type").format[String])
  implicit val doneFormat: Format[Done]                  = Formats.of[String].bimap[Done](_ => "done", _ => Done)
}
