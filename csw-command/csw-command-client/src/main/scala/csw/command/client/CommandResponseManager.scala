package csw.command.client

import akka.actor.typed.ActorRef
import csw.command.client.MiniCRM.CRMMessage
import csw.command.client.MiniCRM.CRMMessage.AddResponse
import csw.params.commands.CommandResponse.SubmitResponse

/**
 * Wrapper API for interacting with Command Response Manager of a component
 */
class CommandResponseManager(val commandResponseManagerActor: ActorRef[CRMMessage], maxSize: Int) {

  /**
   * Add a new command or update an existing command with the provided status
   *
   * @param submitResponse final update for a started command [[csw.params.commands.CommandResponse.SubmitResponse]]
   */
  // fixme: update to subscribers not being done. can be done in cache update key as well
  def updateCommand(submitResponse: SubmitResponse): Unit = commandResponseManagerActor ! AddResponse(submitResponse)

}
