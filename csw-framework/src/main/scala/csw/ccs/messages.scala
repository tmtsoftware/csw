package csw.ccs

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandExecutionResponse
import csw.messages.params.states.CurrentState

sealed trait MultiStateMatcherMsgs
object MultiStateMatcherMsgs {
  sealed trait WaitingMsg                                                                          extends MultiStateMatcherMsgs
  case class StartMatch(replyTo: ActorRef[CommandExecutionResponse], matchers: List[StateMatcher]) extends WaitingMsg
  object StartMatch {
    def apply(replyTo: ActorRef[CommandExecutionResponse], matchers: StateMatcher*): StartMatch =
      StartMatch(replyTo, matchers.toList)
  }

  sealed trait ExecutingMsg                          extends MultiStateMatcherMsgs
  case class StateUpdate(currentState: CurrentState) extends ExecutingMsg
  case object Stop                                   extends ExecutingMsg
}
