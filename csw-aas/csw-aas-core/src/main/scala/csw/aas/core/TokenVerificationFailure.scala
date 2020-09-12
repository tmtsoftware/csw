package csw.aas.core

/**
 * Indicates token verification or decoding attempt was failed
 */
sealed abstract class TokenVerificationFailure(msg: String) extends RuntimeException(msg)

object TokenVerificationFailure {
  case object TokenExpired                                              extends TokenVerificationFailure("token expired")
  final case class InvalidToken(error: String = "Invalid Token Format") extends TokenVerificationFailure(error)
  object InvalidToken {
    def apply(e: Throwable): InvalidToken = new InvalidToken(e.getMessage)
  }
}
