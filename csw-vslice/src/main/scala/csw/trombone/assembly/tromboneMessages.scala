package csw.trombone.assembly

import akka.typed.ActorRef
import csw.messages.CommandMessage.Submit
import csw.messages.RunningMessage.DomainMessage
import csw.messages.ccs.events.EventTime
import csw.messages.params.generics.Parameter
import csw.messages.params.models.Choice
import csw.messages.params.states.CurrentState
import csw.messages.{CommandExecutionResponse, CommandMessage, CommandResponse, SupervisorExternalMessage}
import csw.trombone.assembly.actors.CommandExecutionState
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.assembly.commands.{AssemblyCommand, AssemblyState}

sealed trait FollowCommandMessages
object FollowCommandMessages {
  case class UpdateNssInUse(nssInUse: Parameter[Boolean])                                 extends FollowCommandMessages
  case class UpdateZAandFE(zenithAngle: Parameter[Double], focusError: Parameter[Double]) extends FollowCommandMessages
  case class UpdateTromboneHcd(running: Option[ActorRef[Submit]])                         extends FollowCommandMessages
}

////////////////////////

sealed trait FollowActorMessages extends FollowCommandMessages
object FollowActorMessages {
  case class UpdatedEventData(zenithAngle: Parameter[Double], focusError: Parameter[Double], time: EventTime)
      extends FollowActorMessages
  case class SetElevation(elevation: Parameter[Double])     extends FollowActorMessages
  case class SetZenithAngle(zenithAngle: Parameter[Double]) extends FollowActorMessages
  case object StopFollowing                                 extends FollowActorMessages
}

////////////////////////

sealed trait TrombonePublisherMsg
object TrombonePublisherMsg {
  case class TrombonePublisherMsgE(tromboneState: TromboneState)                     extends TrombonePublisherMsg
  case class AOESWUpdate(naElevation: Parameter[Double], naRange: Parameter[Double]) extends TrombonePublisherMsg
  case class EngrUpdate(focusError: Parameter[Double], stagePosition: Parameter[Double], zenithAngle: Parameter[Double])
      extends TrombonePublisherMsg
  case class AxisStateUpdate(
      axisName: Parameter[String],
      position: Parameter[Int],
      state: Parameter[Choice],
      inLowLimit: Parameter[Boolean],
      inHighLimit: Parameter[Boolean],
      inHome: Parameter[Boolean]
  ) extends TrombonePublisherMsg
  case class AxisStatsUpdate(
      axisName: Parameter[String],
      initCount: Parameter[Int],
      moveCount: Parameter[Int],
      homeCount: Parameter[Int],
      limitCount: Parameter[Int],
      successCount: Parameter[Int],
      failCount: Parameter[Int],
      cancelCount: Parameter[Int]
  ) extends TrombonePublisherMsg
}

///////////////////

sealed trait TromboneControlMsg
object TromboneControlMsg {
  case class UpdateTromboneHcd(running: Option[ActorRef[Submit]]) extends TromboneControlMsg
  case class GoToStagePosition(stagePosition: Parameter[Double])  extends TromboneControlMsg
}

/////////////////////

sealed trait DiagPublisherMessages extends DomainMessage
object DiagPublisherMessages {
  final case class TimeForAxisStats(periodInseconds: Int)                                  extends DiagPublisherMessages
  final case object DiagnosticState                                                        extends DiagPublisherMessages
  final case object OperationsState                                                        extends DiagPublisherMessages
  final case class CurrentStateE(cs: CurrentState)                                         extends DiagPublisherMessages
  final case class UpdateTromboneHcd(running: Option[ActorRef[SupervisorExternalMessage]]) extends DiagPublisherMessages
}

////////////////////
sealed trait TromboneCommandHandlerMsgs
object TromboneCommandHandlerMsgs {
  case class TromboneStateE(tromboneState: TromboneState) extends NotFollowingMsgs with FollowingMsgs with ExecutingMsgs

  sealed trait NotFollowingMsgs extends TromboneCommandHandlerMsgs
  sealed trait FollowingMsgs    extends TromboneCommandHandlerMsgs
  sealed trait ExecutingMsgs    extends TromboneCommandHandlerMsgs

  private[assembly] case class CommandStart(replyTo: ActorRef[CommandResponse]) extends ExecutingMsgs
  private[assembly] case class CommandComplete(replyTo: ActorRef[CommandResponse], result: CommandExecutionResponse)
      extends ExecutingMsgs
}

////////////////////
sealed trait AssemblyCommandHandlerMsgs

sealed trait CommonMsgs extends AssemblyCommandHandlerMsgs
object CommonMsgs {
  case class AssemblyStateE(state: AssemblyState) extends CommonMsgs
}
sealed trait NotFollowingMsgs extends AssemblyCommandHandlerMsgs
sealed trait FollowingMsgs    extends AssemblyCommandHandlerMsgs
sealed trait ExecutingMsgs    extends AssemblyCommandHandlerMsgs

object AssemblyCommandHandlerMsgs {
  case class CommandMessageE(commandMessage: CommandMessage)
      extends NotFollowingMsgs
      with FollowingMsgs
      with ExecutingMsgs
  case class CommandComplete(replyTo: ActorRef[CommandResponse], result: CommandExecutionResponse)
      extends ExecutingMsgs
      with FollowingMsgs
}

case class AssemblyCommandState(mayBeAssemblyCommand: Option[AssemblyCommand],
                                commandExecutionState: CommandExecutionState)
