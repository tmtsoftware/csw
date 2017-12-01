package csw.common.components.command

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.framework.ComponentInfo
import csw.messages.models.PubSub
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, ComponentMessage}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

class ComponentBehaviorFactoryForCommand extends ComponentBehaviorFactory[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers[ComponentDomainMessage] =
    new ComponentHandlerForCommand(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory)
}

class McsAssemblyBehaviorFactory extends ComponentBehaviorFactory[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers[ComponentDomainMessage] =
    new McsAssemblyComponentHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory)
}

class McsHcdBehaviorFactory extends ComponentBehaviorFactory[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers[ComponentDomainMessage] =
    new McsHcdComponentHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory)
}
