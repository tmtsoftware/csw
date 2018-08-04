package csw.framework.internal.supervisor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.scaladsl.ComponentBehaviorFactory
import csw.messages.framework.ComponentInfo
import csw.messages.{ComponentMessage, ContainerIdleMessage, SupervisorMessage}
import csw.services.alarm.api.scaladsl.AlarmService
import csw.services.command.internal.CommandResponseManagerFactory
import csw.services.event.api.scaladsl.EventService
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.LoggerFactory

/**
 * The factory for creating [[akka.actor.typed.scaladsl.MutableBehavior]] of the supervisor of a component
 */
private[framework] object SupervisorBehaviorFactory {

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      eventService: EventService,
      alarmService: AlarmService,
      registrationFactory: RegistrationFactory,
      commandResponseManagerFactory: CommandResponseManagerFactory
  ): Behavior[ComponentMessage] = {

    val componentWiringClass = Class.forName(componentInfo.behaviorFactoryClassName)
    val componentBehaviorFactory =
      componentWiringClass.getDeclaredConstructor().newInstance().asInstanceOf[ComponentBehaviorFactory]
    val loggerFactory = new LoggerFactory(componentInfo.name)

    make(
      containerRef,
      componentInfo,
      locationService,
      eventService,
      alarmService,
      registrationFactory,
      componentBehaviorFactory,
      commandResponseManagerFactory,
      loggerFactory
    )
  }

  // This method is used by test
  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      eventService: EventService,
      alarmService: AlarmService,
      registrationFactory: RegistrationFactory,
      componentBehaviorFactory: ComponentBehaviorFactory,
      commandResponseManagerFactory: CommandResponseManagerFactory,
      loggerFactory: LoggerFactory
  ): Behavior[ComponentMessage] = {
    Behaviors
      .withTimers[SupervisorMessage](
        timerScheduler ⇒
          Behaviors
            .setup[SupervisorMessage](
              ctx =>
                new SupervisorBehavior(
                  ctx,
                  timerScheduler,
                  containerRef,
                  componentInfo,
                  componentBehaviorFactory,
                  commandResponseManagerFactory,
                  registrationFactory,
                  locationService,
                  eventService,
                  alarmService,
                  loggerFactory
              )
          )
      )
      .narrow
  }
}
