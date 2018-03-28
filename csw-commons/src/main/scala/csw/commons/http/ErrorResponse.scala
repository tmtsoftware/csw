package csw.commons.http

import play.api.libs.json.{Json, OFormat}

// Two classes are used just to wrap status code and error message inside "error" key in json representation

/**
 * Internal class used to wrap an ErrorMessage
 */
case class ErrorResponse(error: ErrorMessage)
case object ErrorResponse {
  implicit val errorResponseFormat: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}

/**
 * Internal class representing an error message
 */
case class ErrorMessage(code: Int, message: String)
case object ErrorMessage {
  implicit val errorMessageFormat: OFormat[ErrorMessage] = Json.format[ErrorMessage]
}
