package csw.framework.javadsl

import csw.alarm.client.internal.JAlarmServiceImpl
import csw.command.client.messages.TopLevelActorMessage
import csw.config.client.javadsl.JConfigClientFactory
import csw.event.client.internal.commons.EventServiceAdapter
import csw.framework.models.{ComponentContext, CswContext, JCswContext}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.location.client.extensions.LocationServiceExt.RichLocationService

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
abstract class JComponentBehaviorFactory extends ComponentBehaviorFactory() {

  protected def handlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers = {
    import cswCtx._
    import ctx.executionContext
    jHandlers(
      ctx,
      cswCtx = JCswContext(
        locationService.asJava,
        EventServiceAdapter.asJava(eventService),
        new JAlarmServiceImpl(alarmService),
        timeServiceScheduler,
        loggerFactory.asJava,
        JConfigClientFactory.clientApi(ctx.system, locationService.asJava),
        currentStatePublisher,
        commandResponseManager,
        componentInfo
      )
    )
  }

  protected def jHandlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: JCswContext): JComponentHandlers
}
