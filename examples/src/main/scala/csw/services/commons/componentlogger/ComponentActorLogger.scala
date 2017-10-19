package csw.services.commons.componentlogger

import akka.typed.Behavior
import csw.services.logging.scaladsl.ComponentLogger

//#component-logger-actor
class ComponentActorLogger(_componentName: String) extends ComponentLogger.Actor(_componentName) {

  override def receive: Behavior[_] = ???

}
//#component-logger-actor
