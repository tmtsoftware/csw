package csw.ccs.internal.matchers

sealed trait MatcherResponse
object MatcherResponse {
  case object MatchCompleted                   extends MatcherResponse
  case class MatchFailed(throwable: Throwable) extends MatcherResponse
}
