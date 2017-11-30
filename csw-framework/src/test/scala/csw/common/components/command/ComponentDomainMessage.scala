package csw.common.components.command

import csw.messages.RunningMessage.DomainMessage
import csw.messages.ccs.commands.CommandResponse

sealed trait ComponentDomainMessage extends DomainMessage
object ComponentDomainMessage {
  case class LongCommandCompleted(commandResponse: CommandResponse)   extends ComponentDomainMessage
  case class MediumCommandCompleted(commandResponse: CommandResponse) extends ComponentDomainMessage
  case class ShortCommandCompleted(commandResponse: CommandResponse)  extends ComponentDomainMessage
  case class CommandCompleted(commandResponse: CommandResponse)       extends ComponentDomainMessage
}
