package csw.framework.javadsl

import akka.typed.javadsl.ActorContext
import akka.typed.{scaladsl, ActorRef}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.framework.ComponentInfo
import csw.messages.models.PubSub
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, TopLevelActorMessage}
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService
import csw.services.logging.javadsl.JLoggerFactory
import csw.services.logging.scaladsl.LoggerFactory

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
abstract class JComponentBehaviorFactory extends ComponentBehaviorFactory() {

  protected[framework] def handlers(
      ctx: scaladsl.ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers =
    jHandlers(ctx.asJava, componentInfo, commandResponseManager, pubSubRef, locationService.asJava, loggerFactory.asJava)

  protected[framework] def jHandlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
      locationService: ILocationService,
      loggerFactory: JLoggerFactory
  ): JComponentHandlers
}
