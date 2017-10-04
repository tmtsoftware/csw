package csw.framework.internal.supervisor

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.scaladsl.ComponentBehaviorFactory
import csw.messages.framework.ComponentInfo
import csw.messages.{ContainerIdleMessage, SupervisorExternalMessage, SupervisorMessage}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.internal.LogControlMessages

object SupervisorBehaviorFactory {

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      registrationFactory: RegistrationFactory,
      pubSubBehaviorFactory: PubSubBehaviorFactory,
      adminActorRef: ActorRef[LogControlMessages]
  ): Behavior[SupervisorExternalMessage] = {

    val componentWiringClass     = Class.forName(componentInfo.behaviorFactoryClassName)
    val componentBehaviorFactory = componentWiringClass.newInstance().asInstanceOf[ComponentBehaviorFactory[_]]

    make(
      containerRef,
      componentInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory,
      componentBehaviorFactory,
      adminActorRef
    )
  }

  private[supervisor] def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      registrationFactory: RegistrationFactory,
      pubSubBehaviorFactory: PubSubBehaviorFactory,
      componentBehaviorFactory: ComponentBehaviorFactory[_],
      adminActorRef: ActorRef[LogControlMessages]
  ): Behavior[SupervisorExternalMessage] = {
    Actor
      .withTimers[SupervisorMessage](
        timerScheduler ⇒
          Actor
            .mutable[SupervisorMessage](
              ctx =>
                new SupervisorBehavior(
                  ctx,
                  timerScheduler,
                  containerRef,
                  componentInfo,
                  componentBehaviorFactory,
                  pubSubBehaviorFactory,
                  registrationFactory,
                  locationService,
                  adminActorRef
              )
          )
      )
      .narrow
  }
}
