package csw.framework

import akka.actor.typed.ActorRef
import csw.messages.framework.PubSub.{Publish, PublisherMessage}
import csw.messages.params.states.{CurrentState, StateName}

/**
 * Wrapper API for publishing [[csw.messages.params.states.CurrentState]] of a component
 *
 * @param publisherActor the wrapped actor
 */
class CurrentStatePublisher private[framework] (publisherActor: ActorRef[PublisherMessage[CurrentState, StateName]]) {

  /**
   * Publish [[csw.messages.params.states.CurrentState]] to the subscribed components
   *
   * @param currentState [[csw.messages.params.states.CurrentState]] to be published
   */
  def publish(currentState: CurrentState): Unit = publisherActor ! Publish(currentState)

}
