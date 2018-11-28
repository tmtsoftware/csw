package csw.auth.core

sealed trait TokenVerificationFailure extends Product with Serializable

object TokenVerificationFailure {
  case object TokenExpired                                              extends TokenVerificationFailure
  final case class InvalidToken(error: String = "Invalid Token Format") extends TokenVerificationFailure
}
