package csw.commons.http

import spray.json.RootJsonFormat

// Two classes are used just to wrap status code and error message inside "error" key in json representation
case class ErrorResponse(error: ErrorMessage)
case object ErrorResponse {
  import spray.json.DefaultJsonProtocol._
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse.apply)
}

case class ErrorMessage(code: Int, message: String)
case object ErrorMessage {
  import spray.json.DefaultJsonProtocol._
  implicit val errorMessageFormat: RootJsonFormat[ErrorMessage] = jsonFormat2(ErrorMessage.apply)
}
