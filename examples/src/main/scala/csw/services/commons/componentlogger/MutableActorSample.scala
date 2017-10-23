package csw.services.commons.componentlogger

import akka.typed.Behavior
import akka.typed.scaladsl.ActorContext
import csw.services.logging.scaladsl.ComponentLogger

//#component-logger-mutable-actor
class MutableActorSample(ctx: ActorContext[Any], _componentName: String)
    extends ComponentLogger.MutableActor(ctx, _componentName) {

  override def onMessage(msg: Any): Behavior[Any] = ???

}
//#component-logger-mutable-actor
