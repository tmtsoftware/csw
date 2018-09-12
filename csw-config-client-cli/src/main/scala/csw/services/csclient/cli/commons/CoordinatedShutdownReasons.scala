package csw.services.csclient.cli.commons

import akka.actor.CoordinatedShutdown

object CoordinatedShutdownReasons {
  case object ApplicationFinishedReason extends CoordinatedShutdown.Reason
}
