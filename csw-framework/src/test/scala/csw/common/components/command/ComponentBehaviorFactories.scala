package csw.common.components.command

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.framework.ComponentInfo
import csw.messages.models.PubSub
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, TopLevelActorMessage}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

class ComponentBehaviorFactoryForCommand extends ComponentBehaviorFactory[TopLevelActorDomainMessage] {
  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers[TopLevelActorDomainMessage] =
    new ComponentHandlerForCommand(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory)
}

class McsAssemblyBehaviorFactory extends ComponentBehaviorFactory[TopLevelActorDomainMessage] {
  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers[TopLevelActorDomainMessage] =
    new McsAssemblyComponentHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory)
}

class McsHcdBehaviorFactory extends ComponentBehaviorFactory[TopLevelActorDomainMessage] {
  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers[TopLevelActorDomainMessage] =
    new McsHcdComponentHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory)
}
