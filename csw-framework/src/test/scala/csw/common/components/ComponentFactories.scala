package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.framework.ComponentInfo
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, ComponentMessage, PubSub}
import csw.services.location.scaladsl.LocationService

class SampleComponentBehaviorFactory extends ComponentBehaviorFactory[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[ComponentDomainMessage] =
    new SampleComponentHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService)
}

class ComponentBehaviorFactoryToSimulateFailure extends ComponentBehaviorFactory[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[ComponentDomainMessage] =
    new ComponentHandlerToSimulateFailure(ctx, componentInfo, commandResponseManager, pubSubRef, locationService)
}
