package csw.services.commons.componentlogger

import akka.typed.Behavior
import akka.typed.scaladsl.ActorContext
import csw.services.commons.ComponentDomainMessage
import csw.services.logging.scaladsl.ComponentLogger

//#component-logger-mutable-actor
class MutableActorSample(ctx: ActorContext[ComponentDomainMessage], _componentName: String)
    extends ComponentLogger.MutableActor(ctx, _componentName) {

  override def onMessage(msg: ComponentDomainMessage): Behavior[ComponentDomainMessage] = ???

}
//#component-logger-mutable-actor
