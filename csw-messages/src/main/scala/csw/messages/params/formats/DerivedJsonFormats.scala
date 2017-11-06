package csw.messages.params.formats

import java.lang
import java.time.Instant

import play.api.libs.json._

trait DerivedJsonFormats { self ⇒

  private def formatFactory[S: Writes: Reads, J]: Format[J] =
    Format(implicitly[Reads[S]].asInstanceOf[Reads[J]], implicitly[Writes[S]].asInstanceOf[Writes[J]])

  //scala
  implicit val charFormat: Format[Char] = new Format[Char] {
    override def reads(json: JsValue): JsResult[Char] = json match {
      case JsString(str) if str.length == 1 => JsSuccess(str.head)
      case _                                => JsError("error.expected.char")
    }

    override def writes(o: Char): JsValue = JsString(o.toString)
  }

  //java
  implicit val booleanFormat: Format[lang.Boolean]     = formatFactory[Boolean, java.lang.Boolean]
  implicit val characterFormat: Format[lang.Character] = formatFactory[Char, java.lang.Character]
  implicit val byteFormat: Format[lang.Byte]           = formatFactory[Byte, java.lang.Byte]
  implicit val shortFormat: Format[lang.Short]         = formatFactory[Short, java.lang.Short]
  implicit val longFormat: Format[lang.Long]           = formatFactory[Long, java.lang.Long]
  implicit val integerFormat: Format[lang.Integer]     = formatFactory[Int, java.lang.Integer]
  implicit val floatFormat: Format[lang.Float]         = formatFactory[Float, java.lang.Float]
  implicit val doubleFormat: Format[lang.Double]       = formatFactory[Double, java.lang.Double]
  implicit val timestampFormat: Format[Instant] = new Format[Instant] {
    override def reads(json: JsValue): JsResult[Instant] = JsSuccess(Instant.parse(json.as[String]))
    override def writes(instant: Instant): JsValue       = JsString(instant.toString)
  }
}
