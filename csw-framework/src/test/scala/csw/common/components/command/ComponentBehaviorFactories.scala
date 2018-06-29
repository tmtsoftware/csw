package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.CurrentStatePublisher
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo
import csw.services.command.CommandResponseManager
import csw.services.event.scaladsl.EventService
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

class ComponentBehaviorFactoryForCommand extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: CommandResponseManager,
      currentStatePublisher: CurrentStatePublisher,
      locationService: LocationService,
      eventService: EventService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers =
    new ComponentHandlerForCommand(
      ctx,
      componentInfo,
      commandResponseManager,
      currentStatePublisher,
      locationService,
      eventService,
      loggerFactory
    )
}

class McsAssemblyBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: CommandResponseManager,
      currentStatePublisher: CurrentStatePublisher,
      locationService: LocationService,
      eventService: EventService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers =
    new McsAssemblyComponentHandlers(
      ctx,
      componentInfo,
      commandResponseManager,
      currentStatePublisher,
      locationService,
      eventService,
      loggerFactory
    )
}

class McsHcdBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: CommandResponseManager,
      currentStatePublisher: CurrentStatePublisher,
      locationService: LocationService,
      eventService: EventService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers =
    new McsHcdComponentHandlers(
      ctx,
      componentInfo,
      commandResponseManager,
      currentStatePublisher,
      locationService,
      eventService,
      loggerFactory
    )
}
