package csw.services.event.cli

import play.api.libs.json.JsObject
import ujson.Js
import ujson.play.PlayJson

object EventJsonTransformer {
  def transform(eventJson: Js.Obj, paths: List[String]): Js.Obj = {
    val pathAsLists     = paths.map(_.split("/").toList)
    val jsObject        = eventJson.transform(PlayJson).as[JsObject]
    val transformedJson = JsonSelector(pathAsLists).transform(jsObject)
    PlayJson.transform(transformedJson, Js).obj
  }
}
