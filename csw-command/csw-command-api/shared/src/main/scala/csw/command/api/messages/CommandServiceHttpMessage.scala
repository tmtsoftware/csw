package csw.command.api.messages

import csw.params.commands.ControlCommand
import csw.params.core.models.Id

sealed trait CommandServiceHttpMessage

object CommandServiceHttpMessage {
  case class Validate(controlCommand: ControlCommand) extends CommandServiceHttpMessage
  case class Submit(controlCommand: ControlCommand)   extends CommandServiceHttpMessage
  case class Oneway(controlCommand: ControlCommand)   extends CommandServiceHttpMessage
  case class Query(runId: Id)                         extends CommandServiceHttpMessage
}
