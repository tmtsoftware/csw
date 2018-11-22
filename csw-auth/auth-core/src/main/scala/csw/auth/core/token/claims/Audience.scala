package csw.auth.core.token.claims

import play.api.libs.json._

case class Audience(value: Seq[String])

object Audience {

  def apply(aud: String): Audience = Audience(Seq(aud))

  def apply(): Audience = Audience(Seq.empty)

  implicit val format: Format[Audience] = new Format[Audience] {
    override def writes(obj: Audience): JsValue = obj.value match {
      case head :: Nil ⇒ JsString(head)
      case as          ⇒ JsArray(as.map(JsString))
    }

    override def reads(json: JsValue): JsResult[Audience] = json match {
      case JsNull       ⇒ JsSuccess(Audience())
      case JsString(s)  ⇒ JsSuccess(Audience(s))
      case JsArray(arr) ⇒ JsSuccess(Audience(arr.map(_.as[String])))
      case _            => JsError(JsPath \ "aud", "parsing failed due to invalid value")
    }
  }
}
