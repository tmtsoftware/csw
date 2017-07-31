package csw.trombone.assembly

import akka.typed.ActorRef
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.framework.models.DomainMsg
import csw.common.framework.models.HcdResponseMode.Running
import csw.common.framework.models.RunningHcdMsg.Submit
import csw.param.Events.EventTime
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState
import csw.param.parameters.{ChoiceParameter, GParam}
import csw.param.parameters.primitives.StringParameter
import csw.trombone.assembly.actors.TromboneStateActor.TromboneState

sealed trait FollowCommandMessages
object FollowCommandMessages {
  case class UpdateNssInUse(nssInUse: GParam[Boolean])                              extends FollowCommandMessages
  case class UpdateZAandFE(zenithAngle: GParam[Double], focusError: GParam[Double]) extends FollowCommandMessages
  case class UpdateTromboneHcd(running: Option[ActorRef[Submit]])                   extends FollowCommandMessages
}

////////////////////////

sealed trait FollowActorMessages extends FollowCommandMessages
object FollowActorMessages {
  case class UpdatedEventData(zenithAngle: GParam[Double], focusError: GParam[Double], time: EventTime)
      extends FollowActorMessages
  case class SetElevation(elevation: GParam[Double])     extends FollowActorMessages
  case class SetZenithAngle(zenithAngle: GParam[Double]) extends FollowActorMessages
  case object StopFollowing                              extends FollowActorMessages
}

////////////////////////

sealed trait TrombonePublisherMsg
object TrombonePublisherMsg {
  case class TrombonePublisherMsgE(tromboneState: TromboneState)               extends TrombonePublisherMsg
  case class AOESWUpdate(naElevation: GParam[Double], naRange: GParam[Double]) extends TrombonePublisherMsg
  case class EngrUpdate(focusError: GParam[Double], stagePosition: GParam[Double], zenithAngle: GParam[Double])
      extends TrombonePublisherMsg
  case class AxisStateUpdate(axisName: StringParameter,
                             position: GParam[Int],
                             state: ChoiceParameter,
                             inLowLimit: GParam[Boolean],
                             inHighLimit: GParam[Boolean],
                             inHome: GParam[Boolean])
      extends TrombonePublisherMsg
  case class AxisStatsUpdate(axisName: StringParameter,
                             initCount: GParam[Int],
                             moveCount: GParam[Int],
                             homeCount: GParam[Int],
                             limitCount: GParam[Int],
                             successCount: GParam[Int],
                             failCount: GParam[Int],
                             cancelCount: GParam[Int])
      extends TrombonePublisherMsg
}

///////////////////

sealed trait TromboneControlMsg
object TromboneControlMsg {
  case class UpdateTromboneHcd(running: Option[ActorRef[Submit]]) extends TromboneControlMsg
  case class GoToStagePosition(stagePosition: GParam[Double])     extends TromboneControlMsg
}

/////////////////////

sealed trait DiagPublisherMessages extends DomainMsg
object DiagPublisherMessages {
  final case class TimeForAxisStats(periodInseconds: Int)      extends DiagPublisherMessages
  final case object DiagnosticState                            extends DiagPublisherMessages
  final case object OperationsState                            extends DiagPublisherMessages
  final case class CurrentStateE(cs: CurrentState)             extends DiagPublisherMessages
  final case class UpdateTromboneHcd(running: Option[Running]) extends DiagPublisherMessages
}

////////////////////
sealed trait TromboneCommandHandlerMsgs
object TromboneCommandHandlerMsgs {
  case class TromboneStateE(tromboneState: TromboneState)
      extends NotFollowingMsgs
      with FollowingMsgs
      with ExecutingMsgs

  sealed trait NotFollowingMsgs extends TromboneCommandHandlerMsgs
  sealed trait FollowingMsgs    extends TromboneCommandHandlerMsgs
  sealed trait ExecutingMsgs    extends TromboneCommandHandlerMsgs

  case class Submit(command: Setup, replyTo: ActorRef[CommandResponse])
      extends ExecutingMsgs
      with NotFollowingMsgs
      with FollowingMsgs

  private[assembly] case class CommandStart(replyTo: ActorRef[CommandResponse]) extends ExecutingMsgs
}
