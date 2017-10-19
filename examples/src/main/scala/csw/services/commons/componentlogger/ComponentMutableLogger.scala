package csw.services.commons.componentlogger

import akka.typed.Behavior
import akka.typed.scaladsl.ActorContext
import csw.services.logging.scaladsl.ComponentLogger

//#component-logger-mutable-actor
class ComponentMutableLogger(ctx: ActorContext[_], _componentName: String)
    extends ComponentLogger.MutableActor(ctx, _componentName) {

  override def onMessage(msg: Any): Behavior[_] = ???

}
//#component-logger-mutable-actor
