package csw.trombone.messages

import akka.typed.ActorRef
import csw.common.ccs.CommandStatus.CommandResponse
import csw.trombone.assembly.actors.TromboneStateActor.StateWasSet

sealed trait CommandMsgs
object CommandMsgs {
  case class CommandStart(replyTo: ActorRef[CommandResponse]) extends CommandMsgs
  case object StopCurrentCommand                              extends CommandMsgs
  case class SetStateResponseE(response: StateWasSet)         extends CommandMsgs
}
