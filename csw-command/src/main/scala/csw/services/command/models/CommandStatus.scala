package csw.services.command.models

import csw.messages.commands.CommandResponse
import csw.messages.params.models.Id

/**
 * Represents current state of a command execution
 * @param runId command identifier as a RunId
 * @param currentCmdStatus current command response
 */
case class CommandStatus private[command] (runId: Id, currentCmdStatus: CommandResponse) {
  def withCommandResponse(commandResponse: CommandResponse): CommandStatus = copy(currentCmdStatus = commandResponse)
}
