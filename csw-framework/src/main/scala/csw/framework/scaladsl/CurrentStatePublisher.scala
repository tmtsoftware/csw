package csw.framework.scaladsl

import akka.typed.ActorRef
import csw.messages.models.PubSub.{Publish, PublisherMessage}
import csw.messages.params.states.CurrentState

class CurrentStatePublisher(publisherActor: ActorRef[PublisherMessage[CurrentState]]) {

  def publish(currentState: CurrentState): Unit = publisherActor ! Publish(currentState)

}
