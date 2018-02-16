package tmt.production.component

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.framework.ComponentInfo
import csw.messages.models.PubSub
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, TopLevelActorMessage}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

class SequencerBehaviourFactory extends ComponentBehaviorFactory {
  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers =
    new SequencerHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory)
}
