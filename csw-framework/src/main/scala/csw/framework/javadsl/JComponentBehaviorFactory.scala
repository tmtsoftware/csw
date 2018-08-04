package csw.framework.javadsl

import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.scaladsl
import csw.framework.CurrentStatePublisher
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.alarm.api.scaladsl.AlarmService
import csw.services.alarm.client.internal.JAlarmServiceImpl
import csw.services.command.CommandResponseManager
import csw.services.event.api.javadsl.IEventService
import csw.services.event.api.scaladsl.EventService
import csw.services.event.internal.commons.EventServiceAdapter
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService
import csw.services.logging.javadsl.JLoggerFactory
import csw.services.logging.scaladsl.LoggerFactory

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
abstract class JComponentBehaviorFactory extends ComponentBehaviorFactory() {

  protected def handlers(
      ctx: scaladsl.ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: CommandResponseManager,
      currentStatePublisher: CurrentStatePublisher,
      locationService: LocationService,
      eventService: EventService,
      alarmService: AlarmService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers =
    jHandlers(
      ctx.asJava,
      componentInfo,
      commandResponseManager,
      currentStatePublisher,
      locationService.asJava,
      EventServiceAdapter.asJava(eventService),
      new JAlarmServiceImpl(alarmService),
      loggerFactory.asJava
    )

  protected[framework] def jHandlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: CommandResponseManager,
      currentStatePublisher: CurrentStatePublisher,
      locationService: ILocationService,
      eventService: IEventService,
      alarmService: IAlarmService,
      loggerFactory: JLoggerFactory
  ): JComponentHandlers
}
