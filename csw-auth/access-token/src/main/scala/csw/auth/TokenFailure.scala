package csw.auth

sealed trait TokenFailure

//todo: consider moving ADT cases inside companion object TokenFailure
case object TokenExpired                                                    extends TokenFailure
final case class InvalidTokenFormat(error: String = "Invalid Token Format") extends TokenFailure
case object KidMissing                                                      extends TokenFailure
case object TokenMissing                                                    extends TokenFailure
