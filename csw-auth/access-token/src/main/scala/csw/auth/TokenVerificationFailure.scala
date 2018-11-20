package csw.auth

sealed trait TokenVerificationFailure

object TokenVerificationFailure {
  case object TokenExpired                                              extends TokenVerificationFailure
  final case class InvalidToken(error: String = "Invalid Token Format") extends TokenVerificationFailure
}
