package csw.common.ccs

import akka.typed.ActorRef
import csw.common.ccs.CommandStatus.CommandResponse
import csw.param.CurrentState
import csw.trombone.assembly.actors.TromboneStateActor.StateWasSet

sealed trait MultiStateMatcherMsgs
object MultiStateMatcherMsgs {
  sealed trait WaitingMsg                                                                 extends MultiStateMatcherMsgs
  case class StartMatch(replyTo: ActorRef[CommandResponse], matchers: List[StateMatcher]) extends WaitingMsg
  object StartMatch {
    def apply(replyTo: ActorRef[CommandResponse], matchers: StateMatcher*): StartMatch =
      StartMatch(replyTo, matchers.toList)
  }

  sealed trait ExecutingMsg                          extends MultiStateMatcherMsgs
  case class StateUpdate(currentState: CurrentState) extends ExecutingMsg
  case object Stop                                   extends ExecutingMsg
}

sealed trait CommandMsgs
object CommandMsgs {
  case class CommandStart(replyTo: ActorRef[CommandResponse]) extends CommandMsgs
  case object StopCurrentCommand                              extends CommandMsgs
  case class SetStateResponseE(response: StateWasSet)         extends CommandMsgs
}
