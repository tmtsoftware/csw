package csw.messages.params.formats

import java.lang
import java.time.Instant

import play.api.libs.json._

trait JavaFormats { self ⇒
  //JSON Formats for Java by converting scala types to java types.
//  implicit val booleanFormat: JsonFormat[lang.Boolean] = BooleanJsonFormat.asInstanceOf[JsonFormat[java.lang.Boolean]]
//  implicit val characterFormat: JsonFormat[lang.Character] =
//    CharJsonFormat.asInstanceOf[JsonFormat[java.lang.Character]]
//  implicit val byteFormat: JsonFormat[lang.Byte]     = ByteJsonFormat.asInstanceOf[JsonFormat[java.lang.Byte]]
//  implicit val shortFormat: JsonFormat[lang.Short]   = ShortJsonFormat.asInstanceOf[JsonFormat[java.lang.Short]]
//  implicit val longFormat: JsonFormat[lang.Long]     = LongJsonFormat.asInstanceOf[JsonFormat[java.lang.Long]]
//  implicit val integerFormat: JsonFormat[Integer]    = IntJsonFormat.asInstanceOf[JsonFormat[java.lang.Integer]]
//  implicit val floatFormat: JsonFormat[lang.Float]   = FloatJsonFormat.asInstanceOf[JsonFormat[java.lang.Float]]
//  implicit val doubleFormat: JsonFormat[lang.Double] = DoubleJsonFormat.asInstanceOf[JsonFormat[java.lang.Double]]
//  implicit val timestampFormat: JsonFormat[java.time.Instant] = new JsonFormat[Instant] {
//    override def write(obj: Instant): JsValue = JsString(obj.toString)
//    override def read(json: JsValue): Instant = Instant.parse(json.convertTo[String])
//  }

  implicit val charFormat: Format[Char] = new Format[Char] {
    override def reads(json: JsValue): JsResult[Char] = json match {
      case JsString(str) if str.length == 1 => JsSuccess(str.charAt(0))
      case _                                => JsError("error.expected.char")
    }

    override def writes(o: Char): JsValue = JsString(o.toString)
  }

  implicit val JCharFormat: Format[lang.Character] = new Format[lang.Character] {
    override def reads(json: JsValue): JsResult[lang.Character] = json match {
      case JsString(str) if str.length == 1 => JsSuccess(str.charAt(0))
      case _                                => JsError("error.expected.char")
    }

    override def writes(o: lang.Character): JsValue = JsString(o.toString)
  }

  implicit val booleanWrites: Writes[java.lang.Boolean] =
    implicitly[Writes[Boolean]].asInstanceOf[Writes[java.lang.Boolean]]
  implicit val booleanReads: Reads[java.lang.Boolean] =
    implicitly[Reads[Boolean]].asInstanceOf[Reads[java.lang.Boolean]]

  implicit val characterWrites: Writes[java.lang.Character] = implicitly[Writes[java.lang.Character]]
  implicit val characterReads: Reads[java.lang.Character]   = implicitly[Reads[java.lang.Character]]

  implicit val byteWrites: Writes[java.lang.Byte] = implicitly[Writes[Byte]].asInstanceOf[Writes[java.lang.Byte]]
  implicit val byteReads: Reads[java.lang.Byte]   = implicitly[Reads[Byte]].asInstanceOf[Reads[java.lang.Byte]]

  implicit val shortWrites: Writes[java.lang.Short] = implicitly[Writes[Short]].asInstanceOf[Writes[java.lang.Short]]
  implicit val shortReads: Reads[java.lang.Short]   = implicitly[Reads[Short]].asInstanceOf[Reads[java.lang.Short]]

  implicit val longWrites: Writes[java.lang.Long] = implicitly[Writes[Long]].asInstanceOf[Writes[java.lang.Long]]
  implicit val longReads: Reads[java.lang.Long]   = implicitly[Reads[Long]].asInstanceOf[Reads[java.lang.Long]]

  implicit val integerWrites: Writes[java.lang.Integer] =
    implicitly[Writes[Int]].asInstanceOf[Writes[java.lang.Integer]]
  implicit val integerReads: Reads[java.lang.Integer] = implicitly[Reads[Int]].asInstanceOf[Reads[java.lang.Integer]]

  implicit val floatWrites: Writes[java.lang.Float] = implicitly[Writes[Float]].asInstanceOf[Writes[java.lang.Float]]
  implicit val floatReads: Reads[java.lang.Float]   = implicitly[Reads[Float]].asInstanceOf[Reads[java.lang.Float]]

  implicit val doubleWrites: Writes[java.lang.Double] =
    implicitly[Writes[Double]].asInstanceOf[Writes[java.lang.Double]]
  implicit val doubleReads: Reads[java.lang.Double] = implicitly[Reads[Double]].asInstanceOf[Reads[java.lang.Double]]

  implicit val timestampFormat: Format[Instant] = new Format[Instant] {
    override def reads(json: JsValue): JsResult[Instant] = JsSuccess(Instant.parse(Json.stringify(json)))
    override def writes(instant: Instant): JsValue       = JsString(instant.toString)
  }
}
