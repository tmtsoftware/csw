package csw.trombone.messages

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandExecutionResponse

sealed trait CommandMsgs
object CommandMsgs {
  case class CommandStart(replyTo: ActorRef[CommandExecutionResponse]) extends CommandMsgs
  case object StopCurrentCommand                                       extends CommandMsgs
}
