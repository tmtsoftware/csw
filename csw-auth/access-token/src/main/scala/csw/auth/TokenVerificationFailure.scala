package csw.auth

sealed trait TokenVerificationFailure

//todo: consider moving ADT cases inside companion object TokenFailure
case object TokenExpired                                                    extends TokenVerificationFailure
final case class InvalidTokenFormat(error: String = "Invalid Token Format") extends TokenVerificationFailure
case object KidMissing                                                      extends TokenVerificationFailure
case object TokenMissing                                                    extends TokenVerificationFailure
