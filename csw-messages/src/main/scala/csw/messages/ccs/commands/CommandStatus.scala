package csw.messages.ccs.commands

import csw.messages.params.models.Id

/**
 * Represents current state of a command execution
 * @param runId command identifier as a RunId
 * @param currentCmdStatus current command response
 */
case class CommandStatus private[csw] (runId: Id, currentCmdStatus: CommandResponse) {
  def withCommandResponse(commandResponse: CommandResponse): CommandStatus = copy(currentCmdStatus = commandResponse)
}
