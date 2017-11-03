package csw.framework.scaladsl

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.framework.internal.component.ComponentBehavior
import csw.messages.PubSub.PublisherMessage
import csw.messages.RunningMessage.DomainMessage
import csw.messages.framework.ComponentInfo
import csw.messages.params.states.CurrentState
import csw.messages.{ComponentMessage, FromComponentLifecycleMessage}
import csw.services.location.scaladsl.LocationService

import scala.reflect.ClassTag

/**
 * Implement this factory for creating the behavior representing a component actor
 * @tparam Msg  The type of messages created for domain specific message hierarchy of the component
 */
abstract class ComponentBehaviorFactory[Msg <: DomainMessage: ClassTag] {

  /**
   * Implement this method for providing the component handlers to be used by component actor
   * @param ctx               The Actor Context under which the actor instance of this behavior is created
   * @param componentInfo     Component related information as described in the configuration file
   * @param pubSubRef         The pub sub actor to publish state represented by [[CurrentState]] for this component
   * @param locationService   The single instance of Location service created for a running application
   * @return                  ComponentHandlers to be used by this component
   */
  protected[framework] def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[Msg]

  /**
   * Creates the [[MutableBehavior]] of the component
   * @param componentInfo     Component related information as described in the configuration file
   * @param supervisor        The actor reference of the supervisor actor which created this component
   * @param pubSubRef         The pub sub actor to publish state represented by [[CurrentState]] for this component
   * @param locationService   The single instance of Location service created for a running application
   * @return                  Behavior for component Actor
   */
  def make(
      componentInfo: ComponentInfo,
      supervisor: ActorRef[FromComponentLifecycleMessage],
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService
  ): Behavior[Nothing] =
    Actor
      .mutable[ComponentMessage](
        ctx ⇒
          new ComponentBehavior[Msg](
            ctx,
            componentInfo,
            supervisor,
            handlers(ctx, componentInfo, pubSubRef, locationService),
            locationService
        )
      )
      .narrow
}
