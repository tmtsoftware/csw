package csw.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.framework.internal.component.ComponentBehavior
import csw.messages.framework.ComponentInfo
import csw.messages.{CommandResponseManagerMessage, FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
abstract class ComponentBehaviorFactory {

  /**
   * Implement this method for providing the component handlers to be used by component actor
   * @param ctx                      The Actor Context under which the actor instance of this behavior is created
   * @param componentInfo            Component related information as described in the configuration file
   * @param currentStatePublisher    The pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]]
   *                                 for this component
   * @param locationService          The single instance of Location service created for a running application
   * @return                         ComponentHandlers to be used by this component
   */
  protected[framework] def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      currentStatePublisher: CurrentStatePublisher,
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers

  /**
   * Creates the [[akka.typed.scaladsl.Actor.MutableBehavior]] of the component
   * @param componentInfo            Component related information as described in the configuration file
   * @param supervisor               The actor reference of the supervisor actor which created this component
   * @param currentStatePublisher    The pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]]
   *                                 for this component
   * @param locationService          The single instance of Location service created for a running application
   * @return                         Behavior for component Actor
   */
  def make(
      componentInfo: ComponentInfo,
      supervisor: ActorRef[FromComponentLifecycleMessage],
      currentStatePublisher: CurrentStatePublisher,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): Behavior[Nothing] =
    Actor
      .mutable[TopLevelActorMessage](
        ctx ⇒
          new ComponentBehavior(
            ctx,
            componentInfo,
            supervisor,
            handlers(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory),
            commandResponseManager,
            locationService,
            loggerFactory
        )
      )
      .narrow
}
