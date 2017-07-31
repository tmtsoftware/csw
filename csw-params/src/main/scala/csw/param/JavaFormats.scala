package csw.param

import java.lang

import spray.json.{DefaultJsonProtocol, JsonFormat}

trait JavaFormats extends DefaultJsonProtocol {
  //JSON Formats for Java by converting scala types to java types.
  implicit val integerFormat: JsonFormat[Integer]      = IntJsonFormat.asInstanceOf[JsonFormat[java.lang.Integer]]
  implicit val booleanFormat: JsonFormat[lang.Boolean] = BooleanJsonFormat.asInstanceOf[JsonFormat[java.lang.Boolean]]
  implicit val characterFormat: JsonFormat[lang.Character] =
    BooleanJsonFormat.asInstanceOf[JsonFormat[java.lang.Character]]
}
