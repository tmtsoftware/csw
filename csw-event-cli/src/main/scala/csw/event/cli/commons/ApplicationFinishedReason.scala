package csw.event.cli.commons

import akka.actor.CoordinatedShutdown

case object ApplicationFinishedReason extends CoordinatedShutdown.Reason
