package csw.auth

sealed trait TokenFailure

case object TokenExpired                                              extends TokenFailure
case class InvalidTokenFormat(error: String = "Invalid Token Format") extends TokenFailure
case object KidMissing                                                extends TokenFailure
case object TokenMissing                                              extends TokenFailure
