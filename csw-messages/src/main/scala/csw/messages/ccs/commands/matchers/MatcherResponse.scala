package csw.messages.ccs.commands.matchers

sealed trait MatcherResponse
object MatcherResponses {
  case object MatchCompleted                   extends MatcherResponse
  case class MatchFailed(throwable: Throwable) extends MatcherResponse

  def jMatchCompleted(): MatcherResponse = MatchCompleted
}
