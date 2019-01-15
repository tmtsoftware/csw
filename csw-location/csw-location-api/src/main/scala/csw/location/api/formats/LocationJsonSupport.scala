package csw.location.api.formats

import akka.Done
import csw.params.extensions.Formats
import csw.params.extensions.Formats.MappableFormat
import csw.location.api.models.{AkkaLocation, Registration}
import julienrf.json.derived
import play.api.libs.json.{__, Format, Json, OFormat}

trait LocationJsonSupport extends ActorSystemDependentFormats {
  implicit val akkaLocationFormat: Format[AkkaLocation]  = Json.format[AkkaLocation]
  implicit val registrationFormat: OFormat[Registration] = derived.flat.oformat((__ \ "type").format[String])
  implicit val doneFormat: Format[Done]                  = Formats.of[String].bimap[Done](_ => "done", _ => Done)
}
