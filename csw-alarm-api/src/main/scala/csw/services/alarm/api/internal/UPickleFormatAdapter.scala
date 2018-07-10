package csw.services.alarm.api.internal

import play.api.libs.json._
import ujson.Js
import upickle.default.{ReadWriter => RW, _}

object UPickleFormatAdapter {

  def playJsonToUPickle[T](implicit format: Format[T]): RW[T] = jsValueRW.bimap[T](
    result => Json.toJson(result),
    jsValue => jsValue.as[T]
  )

  private val jsValueRW: RW[JsValue] = readwriter[Js.Value].bimap(
    x => read[Js.Value](x.toString()),
    x => Json.parse(x.toString())
  )
}
