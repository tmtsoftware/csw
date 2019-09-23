package csw.command.client

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.MiniCRM.MiniCRMMessage.AddResponse
import csw.params.commands.CommandResponse.SubmitResponse

/**
 * Wrapper API for interacting with Command Response Manager of a component
 *
 * @param commandResponseManagerActor underlying actor managing command responses for started commands
 * @param actorSystem actor system for allowing sending messages in API
 */
class CommandResponseManager(val commandResponseManagerActor: ActorRef[MiniCRM.CRMMessage])(
    implicit val actorSystem: ActorSystem[_]
) {

  /**
   * Add a new command or update an existing command with the provided status
   *
   * @param submitResponse final update for a started command [[csw.params.commands.CommandResponse.SubmitResponse]]
   */
  def updateCommand(submitResponse: SubmitResponse): Unit = commandResponseManagerActor ! AddResponse(submitResponse)

}
