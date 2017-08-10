package csw.param.formats

import java.lang

import spray.json.{DefaultJsonProtocol, JsonFormat}

trait JavaFormats { self: DefaultJsonProtocol ⇒
  //JSON Formats for Java by converting scala types to java types.
  implicit val booleanFormat: JsonFormat[lang.Boolean] = BooleanJsonFormat.asInstanceOf[JsonFormat[java.lang.Boolean]]
  implicit val characterFormat: JsonFormat[lang.Character] =
    CharJsonFormat.asInstanceOf[JsonFormat[java.lang.Character]]
  implicit val byteFormat: JsonFormat[lang.Byte]     = ByteJsonFormat.asInstanceOf[JsonFormat[java.lang.Byte]]
  implicit val shortFormat: JsonFormat[lang.Short]   = ShortJsonFormat.asInstanceOf[JsonFormat[java.lang.Short]]
  implicit val longFormat: JsonFormat[lang.Long]     = LongJsonFormat.asInstanceOf[JsonFormat[java.lang.Long]]
  implicit val integerFormat: JsonFormat[Integer]    = IntJsonFormat.asInstanceOf[JsonFormat[java.lang.Integer]]
  implicit val floatFormat: JsonFormat[lang.Float]   = FloatJsonFormat.asInstanceOf[JsonFormat[java.lang.Float]]
  implicit val doubleFormat: JsonFormat[lang.Double] = DoubleJsonFormat.asInstanceOf[JsonFormat[java.lang.Double]]
}
