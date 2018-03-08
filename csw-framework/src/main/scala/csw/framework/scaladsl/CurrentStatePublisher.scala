package csw.framework.scaladsl

import akka.typed.ActorRef
import csw.messages.framework.PubSub.{Publish, PublisherMessage}
import csw.messages.params.states.CurrentState

/**
 * Wrapper API for publishing [[csw.messages.params.states.CurrentState]] of a component.
 * @param publisherActor The wrapped actor
 */
//TODO: add doc for significance
class CurrentStatePublisher(publisherActor: ActorRef[PublisherMessage[CurrentState]]) {

  /**
   * Publish [[csw.messages.params.states.CurrentState]] to the subscribed components.
   * @param currentState [[csw.messages.params.states.CurrentState]] to be published
   */
  def publish(currentState: CurrentState): Unit = publisherActor ! Publish(currentState)

}
