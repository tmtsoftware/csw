package csw.logging.client.internal

import play.api.libs.json._

object JsonExtensions {
  implicit class AnyToJson(val x: Any) extends AnyVal {
    def asJson: JsValue = x match {
      case x1: Int              ⇒ JsNumber(x1)
      case x1: Long             ⇒ JsNumber(x1)
      case x1: Float            ⇒ JsNumber(x1.toDouble)
      case x1: Double           ⇒ JsNumber(x1)
      case x: Boolean           ⇒ JsBoolean(x)
      case x: String            ⇒ JsString(x)
      case null                 ⇒ JsNull
      case xs: Seq[Any]         ⇒ JsArray(xs.map(_.asJson))
      case xs: Map[String, Any] ⇒ JsObject(xs.mapValues(_.asJson))
      case _                    ⇒ JsString(x.toString)
    }
  }

  implicit class AnyMapToJson(val xs: Map[String, Any]) extends AnyVal {
    def asJsObject: JsObject = JsObject(xs.mapValues(_.asJson))
  }

  implicit class RichJsObject(val xs: JsObject) extends AnyVal {
    def getString(key: String): String = xs.value.getOrElse(key, JsString("")) match {
      case x: JsString ⇒ x.value
      case x           ⇒ x.toString()
    }

    def contains(key: String): Boolean = xs.keys.contains(key)

  }

}
