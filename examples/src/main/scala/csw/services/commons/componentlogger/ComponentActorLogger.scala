package csw.services.commons.componentlogger

import csw.services.logging.scaladsl.ComponentLogger

class ComponentActorLogger(_componentName: String) extends ComponentLogger.Actor(_componentName) {
  override def receive: Nothing = ???
}
