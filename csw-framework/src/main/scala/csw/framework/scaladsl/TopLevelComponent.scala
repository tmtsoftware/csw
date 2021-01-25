package csw.framework.scaladsl

import akka.actor.typed.{ActorRef, Behavior}
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandResponse.{SubmitResponse, ValidateCommandResponse}
import csw.params.commands.ControlCommand
import csw.params.core.models.Id

object TopLevelComponent {

  sealed trait TopLevelComponentMessage extends akka.actor.NoSerializationVerificationNeeded

  sealed trait InitializeMessage extends TopLevelComponentMessage
  final case class Initialize(replyTo: ActorRef[InitializeResponse]) extends InitializeMessage

  sealed trait InitializeResponse extends TopLevelComponentMessage
  final case class InitializeSuccess(running: Behavior[RunningMessage]) extends InitializeResponse
  final case object InitializeFailureStop                               extends InitializeResponse
  final case object InitializeFailureRestart                            extends InitializeResponse

  sealed trait ShutdownResponse extends TopLevelComponentMessage
  final case object ShutdownSuccessful extends ShutdownResponse
  final case object ShutdownFailed extends ShutdownResponse

  sealed trait RunningMessage extends TopLevelComponentMessage
  final case class Validate2(runId: Id, cmd: ControlCommand, svr: ActorRef[ValidateCommandResponse]) extends RunningMessage
  final case class Oneway2(runId: Id, cmd: ControlCommand) extends RunningMessage
  final case class Submit2(runId: Id, cmd: ControlCommand, svr: ActorRef[SubmitResponse]) extends RunningMessage
  final case class Shutdown2(svr: ActorRef[ShutdownResponse]) extends RunningMessage
  final case class TrackingEventReceived2(trackingEvent: TrackingEvent) extends RunningMessage


  sealed trait OnlineResponse extends TopLevelComponentMessage
  final case object OnlineSuccess extends OnlineResponse
  final case object OnlineFailure extends OnlineResponse
  final case object OfflineSuccess extends OnlineResponse
  final case object OfflineFailure extends OnlineResponse

  final case class GoOnline2(svr: ActorRef[OnlineResponse]) extends RunningMessage
  final case class GoOffline2(svr: ActorRef[OnlineResponse]) extends RunningMessage

  /*
  trait Supervisor2Codecs extends ParamCodecs with LocationCodecs with ActorCodecs {
    implicit lazy val onlineResponseCodec = deriveAllCodecs[OnlineResponse]
    implicit lazy val shutdownResponseCodec = deriveAllCodecs[ShutdownResponse]
    implicit lazy val runningMsgCodec = deriveAllCodecs[RunningMessage]

    implicit lazy val behaviorCode:Codec[Behavior[Int]] = MapBasedCodecs.deriveCodec

    implicit lazy val initializeResponseCodec = deriveAllCodecs[InitializeResponse]



    //implicit lazy val runningMsgCodec: Codec[RunningMessage] = deriveCodec
    //implicit lazy val initializeCodec: Codec[InitializeMessages] = deriveCodec
    //implicit lazy val tlaStartCodec: Codec[TLAStart]         = deriveCodec
  }

   */

}
