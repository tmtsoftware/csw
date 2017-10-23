package csw.services.commons.componentlogger

import csw.services.logging.scaladsl.ComponentLogger

//#component-logger-actor
class ActorSample(_componentName: String) extends ComponentLogger.Actor(_componentName) {

  override def receive: Nothing = ???
}
//#component-logger-actor
