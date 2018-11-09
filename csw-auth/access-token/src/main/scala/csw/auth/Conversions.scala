package csw.auth

import play.api.libs.json.JsError

import scala.language.implicitConversions
import scala.util.Failure

private[auth] object Conversions {
  implicit def toFailure[T](jsError: JsError): Failure[T] = {
    val message = jsError.errors
      .map {
        case (jsPath, es) =>
          (jsPath.toString(), es.flatMap(_.messages).mkString("\n"))
      }
      .map {
        case (path, es) => s"--------------\n$path\n\n$es"
      }
      .mkString("\n")
    Failure(new RuntimeException(message))
  }
}
