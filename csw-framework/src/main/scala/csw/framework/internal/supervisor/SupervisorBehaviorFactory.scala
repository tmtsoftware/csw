package csw.framework.internal.supervisor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.messages.framework.ComponentInfo
import csw.messages.scaladsl.{ComponentMessage, ContainerIdleMessage, SupervisorMessage}
import csw.services.command.internal.CommandResponseManagerFactory
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
      registrationFactory: RegistrationFactory,
      commandResponseManagerFactory: CommandResponseManagerFactory
  ): Behavior[ComponentMessage] = {

    val loggerFactory = new LoggerFactory(componentInfo.name)

    make(
      containerRef,
      componentInfo,
      locationService,
      registrationFactory,
      commandResponseManagerFactory,
      loggerFactory
    )
  }

  // This method is used by test
  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      registrationFactory: RegistrationFactory,
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
                  commandResponseManagerFactory,
                  registrationFactory,
                  locationService,
                  loggerFactory
              )
          )
      )
      .narrow
  }
}
