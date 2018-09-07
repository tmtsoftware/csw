package csw.framework.javadsl

import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.scaladsl
import csw.framework.models.{CswContext, JCswContext}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo
import csw.services.alarm.client.internal.JAlarmServiceImpl
import csw.services.event.internal.commons.EventServiceAdapter

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
abstract class JComponentBehaviorFactory extends ComponentBehaviorFactory() {

  //TODO
  protected def handlers(
      ctx: scaladsl.ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      cswCtx: CswContext
  ): ComponentHandlers =
    jHandlers(
      ctx.asJava,
      componentInfo,
      JCswContext(
        cswCtx.locationService.asJava,
        EventServiceAdapter.asJava(cswCtx.eventService),
        new JAlarmServiceImpl(cswCtx.alarmService),
        cswCtx.loggerFactory.asJava,
        cswCtx.commandResponseManager,
        cswCtx.currentStatePublisher
      )
    )

  protected[framework] def jHandlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      cswCtx: JCswContext
  ): JComponentHandlers
}
