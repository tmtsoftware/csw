package csw.services.commons.componentlogger

import csw.services.logging.scaladsl.FrameworkLogger

//#component-logger-actor
class ActorSample(_componentName: String) extends FrameworkLogger.Actor(_componentName) {

  override def receive: Nothing = ???
}
//#component-logger-actor
