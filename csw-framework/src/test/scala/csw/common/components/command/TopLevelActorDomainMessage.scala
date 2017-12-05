package csw.common.components.command

import csw.messages.RunningMessage.DomainMessage
import csw.messages.ccs.commands.CommandResponse

sealed trait TopLevelActorDomainMessage extends DomainMessage
object TopLevelActorDomainMessage {
  case class LongCommandCompleted(commandResponse: CommandResponse)   extends TopLevelActorDomainMessage
  case class MediumCommandCompleted(commandResponse: CommandResponse) extends TopLevelActorDomainMessage
  case class ShortCommandCompleted(commandResponse: CommandResponse)  extends TopLevelActorDomainMessage
  case class CommandCompleted(commandResponse: CommandResponse)       extends TopLevelActorDomainMessage
}
