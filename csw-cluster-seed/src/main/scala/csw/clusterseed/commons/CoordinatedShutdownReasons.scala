package csw.clusterseed.commons

import akka.actor.CoordinatedShutdown

object CoordinatedShutdownReasons {
  case class FailureReason(ex: Throwable) extends CoordinatedShutdown.Reason
}
