package csw.event.cli.utils

import csw.params.core.formats.ParamCodecs._
import csw.params.events.{Event, ObserveEvent, SystemEvent}
import io.bullet.borer
import play.api.libs.json.{JsObject, Json}

object EventJsonTransformer {

  def transform(eventJson: JsObject, paths: List[String]): JsObject = {
    val pathAsLists         = paths.map(_.split("/").toList)
    val event               = borer.Json.decode(eventJson.toString().getBytes("utf8")).to[Event].value
    val transformedParamSet = PathSelector(pathAsLists).transform(event.paramSet)
    val transformedEvent: Event = event match {
      case e: ObserveEvent => e.copy(paramSet = transformedParamSet)
      case e: SystemEvent  => e.copy(paramSet = transformedParamSet)
    }
    Json.parse(borer.Json.encode(transformedEvent).toUtf8String).as[JsObject]
  }

}
