package csw.command.client

import akka.actor.typed.ActorRef
import csw.command.client.CommandResponseManagerActor.CRMMessage
import csw.command.client.CommandResponseManagerActor.CRMMessage.{AddResponse, UpdateResponse}
import csw.params.commands.CommandResponse.SubmitResponse

/**
 * Wrapper API for interacting with Command Response Manager of a component
 */
class CommandResponseManager(val commandResponseManagerActor: ActorRef[CRMMessage]) {

  /**
   * Add a new command or update an existing command with the provided status
   *
   * @param submitResponse final update for a started command [[csw.params.commands.CommandResponse.SubmitResponse]]
   */
  def updateCommand(submitResponse: SubmitResponse): Unit = commandResponseManagerActor ! UpdateResponse(submitResponse)

  private[csw] def addCommand(submitResponse: SubmitResponse): Unit = commandResponseManagerActor ! AddResponse(submitResponse)

}
