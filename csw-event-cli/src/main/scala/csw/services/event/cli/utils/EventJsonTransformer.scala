package csw.services.event.cli.utils

import play.api.libs.json.JsObject

object EventJsonTransformer {
  def transform(eventJson: JsObject, paths: List[String]): JsObject = {
    val pathAsLists = paths.map(_.split("/").toList)
    JsonSelector(pathAsLists).transform(eventJson)
  }
}
