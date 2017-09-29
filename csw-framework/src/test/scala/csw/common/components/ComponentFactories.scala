package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.models.ComponentInfo
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.states.CurrentState
import csw.messages.{ComponentMessage, PubSub}
import csw.services.location.scaladsl.LocationService

class SampleComponentBehaviorFactory extends ComponentBehaviorFactory[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[ComponentDomainMessage] =
    new SampleComponentHandlers(ctx, componentInfo, pubSubRef, locationService)
}

class ComponentBehaviorFactoryToSimulateFailure extends ComponentBehaviorFactory[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[ComponentDomainMessage] =
    new ComponentHandlerToSimulateFailure(ctx, componentInfo, pubSubRef, locationService)
}
