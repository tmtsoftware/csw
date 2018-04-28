package csw.framework.scaladsl

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.internal.component.ComponentBehavior
import csw.messages.framework.ComponentInfo
import csw.messages.scaladsl.{FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

/**
 * Factory object for creating the Top Level Actor behavior
 */
object ComponentBehaviorFactory {

  /**
   * Creates the [[akka.actor.typed.scaladsl.MutableBehavior]] of the component
   *
   * @param componentInfo component related information as described in the configuration file
   * @param supervisor the actor reference of the supervisor actor which created this component
   * @param currentStatePublisher the pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]]
   *                              for this component
   * @param commandResponseManager actor responsible for managing responses of incoming commands
   * @param locationService the single instance of Location service created for a running application
   * @param loggerFactory factory method for creating logging facility instances
   * @return behavior for component Actor
   */
  private[framework] def make(
      componentInfo: ComponentInfo,
      supervisor: ActorRef[FromComponentLifecycleMessage],
      currentStatePublisher: CurrentStatePublisher,
      commandResponseManager: CommandResponseManager,
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): Behavior[Nothing] =
    Behaviors
      .setup[TopLevelActorMessage](
        ctx ⇒
          new ComponentBehavior(
            ctx,
            componentInfo,
            supervisor,
            Class
              .forName(componentInfo.handlersClassName)
              .getDeclaredConstructor(
                classOf[ActorContext[TopLevelActorMessage]],
                classOf[ComponentInfo],
                classOf[CommandResponseManager],
                classOf[CurrentStatePublisher],
                classOf[LocationService],
                classOf[LoggerFactory]
              )
              .newInstance(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory)
              .asInstanceOf[ComponentHandlers],
            commandResponseManager,
            locationService,
            loggerFactory
        )
      )
      .narrow
}
