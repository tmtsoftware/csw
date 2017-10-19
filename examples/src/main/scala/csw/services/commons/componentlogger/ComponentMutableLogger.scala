package csw.services.commons.componentlogger

import akka.typed.scaladsl.ActorContext
import csw.services.logging.scaladsl.ComponentLogger

class ComponentMutableLogger(ctx: ActorContext[_], _componentName: String)
    extends ComponentLogger.MutableActor(ctx, _componentName) {
  override def onMessage(msg: Any): Nothing = ???
}
