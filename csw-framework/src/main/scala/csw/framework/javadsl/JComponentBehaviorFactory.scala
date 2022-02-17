package csw.framework.javadsl

import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.scaladsl
import csw.alarm.client.internal.extensions.AlarmServiceExt.RichAlarmService
import csw.command.client.messages.TopLevelActorMessage
import csw.config.client.javadsl.JConfigClientFactory
import csw.event.client.internal.commons.EventServiceExt.RichEventService
import csw.framework.models.{CswContext, JCswContext}
import csw.framework.scaladsl.{ClassHelpers, ComponentBehaviorFactory, ComponentHandlers}
import csw.location.client.extensions.LocationServiceExt.RichLocationService

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
abstract class JComponentBehaviorFactory extends ComponentBehaviorFactory() {

  protected def handlers(ctx: scaladsl.ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers = {
    import cswCtx.*
    import ctx.executionContext
    jHandlers(
      ctx.asJava,
      JCswContext(
        locationService.asJava,
        eventService.asJava,
        alarmService.asJava,
        timeServiceScheduler,
        loggerFactory.asJava,
        JConfigClientFactory.clientApi(ctx.system, locationService.asJava),
        currentStatePublisher,
        commandResponseManager,
        componentInfo
      )
    )
  }

  protected[framework] def jHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: JCswContext): JComponentHandlers
}

object JComponentBehaviorFactory {
  private val jComponentHandlerArgsType = Seq(classOf[ActorContext[TopLevelActorMessage]], classOf[JCswContext])
  private val jComponentHandlersConstructor =
    ClassHelpers.getConstructorFor(classOf[JComponentHandlers], JComponentBehaviorFactory.jComponentHandlerArgsType)

  private[framework] def make(componentHandlerClass: Class[?]): JComponentBehaviorFactory = { (ctx, cswCtx) =>
    ClassHelpers
      .getConstructorFor(componentHandlerClass, jComponentHandlerArgsType)
      .newInstance(ctx, cswCtx)
      .asInstanceOf[JComponentHandlers]
  }

  // verify input class is assignable from JComponentHandler class && it's constructor has required parameters.
  private[framework] def isValid(handlerClass: Class[?]): Boolean =
    ClassHelpers.verifyClass(handlerClass, JComponentBehaviorFactory.jComponentHandlersConstructor)
}
