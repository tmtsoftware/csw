package csw.auth

import play.api.libs.json.JsError

import scala.language.implicitConversions

private[auth] object Conversions {
  implicit def allErrorMessages(jsError: JsError): String = {
    val message = jsError.errors
      .map {
        case (jsPath, es) =>
          (jsPath.toString(), es.flatMap(_.messages).mkString("\n"))
      }
      .map {
        case (path, es) => s"--------------\n$path\n\n$es"
      }
      .mkString("\n")
    message
  }
}
