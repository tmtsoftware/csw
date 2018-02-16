package csw.framework.javadsl

import akka.typed.javadsl.ActorContext
import akka.typed.{scaladsl, ActorRef}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers, CurrentStatePublisher}
import csw.messages.framework.ComponentInfo
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
      currentStatePublisher: CurrentStatePublisher,
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers =
    jHandlers(
      ctx.asJava,
      componentInfo,
      commandResponseManager,
      currentStatePublisher,
      locationService.asJava,
      loggerFactory.asJava
    )

  protected[framework] def jHandlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      currentStatePublisher: CurrentStatePublisher,
      locationService: ILocationService,
      loggerFactory: JLoggerFactory
  ): JComponentHandlers
}
