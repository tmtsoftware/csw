package csw.auth

import play.api.libs.json.{JsError, JsPath}

import scala.language.implicitConversions

private[auth] object Conversions {
  implicit def allErrorMessages(jsError: JsError): String =
    jsError.errors
      .map {
        case (jsPath: JsPath, es) =>
          s"""
            |Error at path: $jsPath
            |Validation errors: ${es.flatMap(_.messages).mkString("\n")}
          """.stripMargin
      }
      .mkString("\n")
}
