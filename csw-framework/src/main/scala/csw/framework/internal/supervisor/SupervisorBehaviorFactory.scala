package csw.framework.internal.supervisor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.scaladsl.ComponentBehaviorFactory
import csw.messages.framework.ComponentInfo
import csw.messages.scaladsl.{ComponentMessage, ContainerIdleMessage, SupervisorMessage}
import csw.services.command.internal.CommandResponseManagerFactory
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.LoggerFactory

/**
 * The factory for creating [[akka.actor.typed.scaladsl.Behaviors.MutableBehavior]] of the supervisor of a component
 */
//TODO: add doc to explain significance of why we have multiple methods
private[framework] object SupervisorBehaviorFactory {

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      registrationFactory: RegistrationFactory,
      pubSubBehaviorFactory: PubSubBehaviorFactory,
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
      registrationFactory,
      pubSubBehaviorFactory,
      componentBehaviorFactory,
      commandResponseManagerFactory,
      loggerFactory
    )
  }

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      registrationFactory: RegistrationFactory,
      pubSubBehaviorFactory: PubSubBehaviorFactory,
      componentBehaviorFactory: ComponentBehaviorFactory,
      commandResponseManagerFactory: CommandResponseManagerFactory,
      loggerFactory: LoggerFactory
  ): Behavior[ComponentMessage] = {
    Behaviors
      .withTimers[SupervisorMessage](
        timerScheduler ⇒
          Behaviors
            .mutable[SupervisorMessage](
              ctx =>
                new SupervisorBehavior(
                  ctx,
                  timerScheduler,
                  containerRef,
                  componentInfo,
                  componentBehaviorFactory,
                  pubSubBehaviorFactory,
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
