package csw.ccs.models

import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId

case class CommandStatus(runId: RunId, currentCmdStatus: CommandResponse) {
  def withCommandResponse(commandResponse: CommandResponse): CommandStatus = copy(currentCmdStatus = commandResponse)
}
