package csw.aas.core

sealed trait TokenVerificationFailure extends Product with Serializable

object TokenVerificationFailure {
  case object TokenExpired extends TokenVerificationFailure
  final case class InvalidToken(error: String = "Invalid Token Format", ex: Option[Throwable] = None)
      extends TokenVerificationFailure
  object InvalidToken {
    def apply(e: Throwable): InvalidToken = new InvalidToken(e.getMessage, Some(e))
  }
}
