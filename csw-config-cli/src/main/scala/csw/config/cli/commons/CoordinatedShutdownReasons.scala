package csw.config.cli.commons

import akka.actor.CoordinatedShutdown

object CoordinatedShutdownReasons {
  case object ApplicationFinishedReason extends CoordinatedShutdown.Reason
}
