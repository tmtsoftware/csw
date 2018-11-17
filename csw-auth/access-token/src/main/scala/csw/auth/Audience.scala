package csw.auth
import play.api.libs.json._

case class Audience(value: Seq[String])

object Audience {

  def apply(aud: String): Audience = new Audience(Seq(aud))

  def apply(): Audience = new Audience(Seq.empty)

  implicit val format: Format[Audience] = new Format[Audience] {
    override def writes(obj: Audience): JsValue = obj.value match {
      case List(head)      => JsString(head)
      case l: List[String] => JsArray(l.map(JsString))
    }

    override def reads(json: JsValue): JsResult[Audience] = json match {
      case JsString(s)  => JsSuccess(Audience(s))
      case JsArray(arr) => JsSuccess(Audience(arr.map(_.as[String])))
      case JsNull       => JsSuccess(Audience())
      case _            => JsError(JsPath \ "aud", "parsing failed due to invalid value")
    }
  }
}
