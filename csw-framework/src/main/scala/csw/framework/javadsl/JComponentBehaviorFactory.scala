package csw.framework.javadsl

import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.scaladsl
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import csw.framework.models.{CswServices, JCswServices}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo
import csw.services.alarm.client.internal.JAlarmServiceImpl
import csw.services.config.client.javadsl.JConfigClientFactory
import csw.services.event.internal.commons.EventServiceAdapter

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
abstract class JComponentBehaviorFactory extends ComponentBehaviorFactory() {

  //TODO
  protected def handlers(
      ctx: scaladsl.ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      cswServices: CswServices
  ): ComponentHandlers = {
    import cswServices._
    jHandlers(
      ctx.asJava,
      componentInfo,
      JCswServices(
        locationService.asJava,
        EventServiceAdapter.asJava(eventService),
        new JAlarmServiceImpl(alarmService),
        loggerFactory.asJava,
        JConfigClientFactory.clientApi(ctx.system.toUntyped, locationService.asJava),
        commandResponseManager,
        currentStatePublisher
      )
    )
  }

  protected[framework] def jHandlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      cswServices: JCswServices
  ): JComponentHandlers
}
