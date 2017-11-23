package csw.services.commons.componentlogger

import akka.typed.Behavior
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.services.commons.ComponentDomainMessage
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

//#component-logger-mutable-actor
class MutableActorSample(ctx: ActorContext[ComponentDomainMessage], _componentName: String)
    extends Actor.MutableBehavior[ComponentDomainMessage] {

  val log: Logger = new LoggerFactory(_componentName).getLogger(ctx)

  override def onMessage(msg: ComponentDomainMessage): Behavior[ComponentDomainMessage] = ???

}
//#component-logger-mutable-actor
