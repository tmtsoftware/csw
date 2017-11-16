package csw.trombone.messages

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandResponse

sealed trait CommandMsgs
object CommandMsgs {
  case class CommandStart(replyTo: ActorRef[CommandResponse]) extends CommandMsgs
  case object StopCurrentCommand                              extends CommandMsgs
}
