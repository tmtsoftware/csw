package csw.common.components.command

import csw.messages.RunningMessage.DomainMessage
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId

sealed trait ComponentDomainMessage extends DomainMessage
object ComponentDomainMessage {
  case class LongCommandCompleted(runId: RunId, commandResponse: CommandResponse)   extends ComponentDomainMessage
  case class MediumCommandCompleted(runId: RunId, commandResponse: CommandResponse) extends ComponentDomainMessage
  case class ShortCommandCompleted(runId: RunId, commandResponse: CommandResponse)  extends ComponentDomainMessage
}
