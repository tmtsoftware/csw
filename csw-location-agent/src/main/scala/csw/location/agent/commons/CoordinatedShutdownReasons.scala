package csw.location.agent.commons

import akka.actor.CoordinatedShutdown

object CoordinatedShutdownReasons {
  case object ProcessTerminated           extends CoordinatedShutdown.Reason
  case class FailureReason(ex: Throwable) extends CoordinatedShutdown.Reason
}
