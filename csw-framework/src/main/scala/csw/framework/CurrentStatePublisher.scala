package csw.framework

import akka.actor.typed.ActorRef
import csw.command.models.framework.PubSub.{Publish, PublisherMessage}
import csw.params.core.states.CurrentState

/**
 * Wrapper API for publishing [[csw.params.core.states.CurrentState]] of a component
 *
 * @param publisherActor the wrapped actor
 */
class CurrentStatePublisher private[framework] (val publisherActor: ActorRef[PublisherMessage[CurrentState]]) {

  /**
   * Publish [[csw.params.core.states.CurrentState]] to the subscribed components
   *
   * @param currentState [[csw.params.core.states.CurrentState]] to be published
   */
  def publish(currentState: CurrentState): Unit = publisherActor ! Publish(currentState)

}
