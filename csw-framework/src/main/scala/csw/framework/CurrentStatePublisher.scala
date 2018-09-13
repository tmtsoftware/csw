package csw.framework

import akka.actor.typed.ActorRef
import csw.command.models.framework.PubSub.{Publish, PublisherMessage}
import csw.messages.params.states.CurrentState

/**
 * Wrapper API for publishing [[csw.messages.params.states.CurrentState]] of a component
 *
 * @param publisherActor the wrapped actor
 */
class CurrentStatePublisher private[framework] (val publisherActor: ActorRef[PublisherMessage[CurrentState]]) {

  /**
   * Publish [[csw.messages.params.states.CurrentState]] to the subscribed components
   *
   * @param currentState [[csw.messages.params.states.CurrentState]] to be published
   */
  def publish(currentState: CurrentState): Unit = publisherActor ! Publish(currentState)

}
