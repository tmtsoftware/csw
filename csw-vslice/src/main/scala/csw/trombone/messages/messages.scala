package csw.trombone.messages

import akka.typed.ActorRef
import csw.messages.messages.CommandExecutionResponse
import csw.trombone.assembly.actors.TromboneStateActor.StateWasSet

sealed trait CommandMsgs
object CommandMsgs {
  case class CommandStart(replyTo: ActorRef[CommandExecutionResponse]) extends CommandMsgs
  case object StopCurrentCommand                                       extends CommandMsgs
  case class SetStateResponseE(response: StateWasSet)                  extends CommandMsgs
}
