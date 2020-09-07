package csw.commons.http

// Two classes are used just to wrap status code and error message inside "error" key in json representation

/**
 * Internal class used to wrap an ErrorMessage
 */
case class ErrorResponse(error: ErrorMessage)

/**
 * Internal class representing an error message
 */
case class ErrorMessage(message: String, _type: Option[String])
