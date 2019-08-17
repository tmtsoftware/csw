package csw.framework

import akka.actor.typed.ActorRef
import csw.command.client.models.framework.PubSub.{Publish, PublisherMessage}
import csw.params.commands.CommandResponse.SubmitResponse

/**
 * Wrapper API for publishing [[csw.params.commands.CommandResponse.SubmitResponse]] for long-running commands
 *
 * @param publisherActor the wrapped actor
 */
class CommandUpdatePublisher private[framework] (val publisherActor: ActorRef[PublisherMessage[SubmitResponse]]) {

  /**
   * Publish [[csw.params.commands.CommandResponse.SubmitResponse]] to the subscribed components
   *
   * @param commandUpdate [[csw.params.commands.CommandResponse.SubmitResponse]] to be published
   */
  def update(commandUpdate: SubmitResponse): Unit = {
    publisherActor ! Publish[SubmitResponse](commandUpdate)
  }

}
