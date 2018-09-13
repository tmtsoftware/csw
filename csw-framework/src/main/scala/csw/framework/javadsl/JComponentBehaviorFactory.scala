package csw.framework.javadsl

import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.scaladsl
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import csw.command.messages.TopLevelActorMessage
import csw.framework.models.{CswContext, JCswContext}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.services.alarm.client.internal.JAlarmServiceImpl
import csw.services.config.client.javadsl.JConfigClientFactory
import csw.services.event.internal.commons.EventServiceAdapter

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
abstract class JComponentBehaviorFactory extends ComponentBehaviorFactory() {

  //TODO
  protected def handlers(ctx: scaladsl.ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers = {
    import cswCtx._
    jHandlers(
      ctx.asJava,
      JCswContext(
        locationService.asJava,
        EventServiceAdapter.asJava(eventService),
        new JAlarmServiceImpl(alarmService),
        loggerFactory.asJava,
        JConfigClientFactory.clientApi(ctx.system.toUntyped, locationService.asJava),
        commandResponseManager,
        currentStatePublisher,
        componentInfo
      )
    )
  }

  protected[framework] def jHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: JCswContext): JComponentHandlers
}
