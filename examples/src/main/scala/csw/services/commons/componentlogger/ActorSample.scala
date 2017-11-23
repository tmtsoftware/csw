package csw.services.commons.componentlogger

import akka.actor.Actor
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

//#component-logger-actor
class ActorSample(_componentName: String) extends Actor {
  val log: Logger = new LoggerFactory(_componentName).getLogger(context)

  override def receive: Nothing = ???
}
//#component-logger-actor
