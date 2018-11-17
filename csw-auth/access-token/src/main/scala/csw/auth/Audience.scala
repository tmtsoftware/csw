package csw.auth

import play.api.libs.json._

case class Audience(aud: List[String])

object Audience {

  implicit val format: Format[Audience] = new Format[Audience] {
    override def writes(obj: Audience): JsValue = JsArray(obj.aud.map(JsString))

    override def reads(json: JsValue): JsResult[Audience] = json match {
      case JsString(s)  ⇒ JsSuccess(Audience(List(s)))
      case JsArray(arr) ⇒ JsSuccess(Audience(arr.toList.map(_.as[String])))
      case _            ⇒ JsError("Unable to parse")
    }
  }

}
