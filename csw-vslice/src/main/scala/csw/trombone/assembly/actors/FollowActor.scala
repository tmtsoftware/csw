package csw.trombone.assembly.actors

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.Events.EventTime
import csw.param.parameters.Parameter
import csw.trombone.assembly.AssemblyContext.{TromboneCalculationConfig, TromboneControlConfig}
import csw.trombone.assembly.FollowActorMessages.{SetElevation, SetZenithAngle, StopFollowing, UpdatedEventData}
import csw.trombone.assembly.TromboneControlMsg.GoToStagePosition
import csw.trombone.assembly.TrombonePublisherMsg.{AOESWUpdate, EngrUpdate}
import csw.trombone.assembly._

object FollowActor {
  def make(
      ac: AssemblyContext,
      initialElevation: Parameter[Double],
      inNSSMode: Parameter[Boolean],
      tromboneControl: Option[ActorRef[TromboneControlMsg]],
      aoPublisher: Option[ActorRef[TrombonePublisherMsg]],
      engPublisher: Option[ActorRef[TrombonePublisherMsg]]
  ): Behavior[FollowActorMessages] =
    Actor.mutable(
      ctx ⇒ new FollowActor(ctx, ac, initialElevation, inNSSMode, tromboneControl, aoPublisher, engPublisher)
    )
}

class FollowActor(
    ctx: ActorContext[FollowActorMessages],
    ac: AssemblyContext,
    val initialElevation: Parameter[Double],
    val inNSSMode: Parameter[Boolean],
    val tromboneControl: Option[ActorRef[TromboneControlMsg]],
    val aoPublisher: Option[ActorRef[TrombonePublisherMsg]],
    val engPublisher: Option[ActorRef[TrombonePublisherMsg]]
) extends MutableBehavior[FollowActorMessages] {

  import Algorithms._
  import ac._

  val calculationConfig: TromboneCalculationConfig = ac.calculationConfig
  val controlConfig: TromboneControlConfig         = ac.controlConfig

  val initialFocusError: Parameter[Double]  = focusErrorKey  -> 0.0 withUnits focusErrorUnits
  val initialZenithAngle: Parameter[Double] = zenithAngleKey -> 0.0 withUnits zenithAngleUnits
  val nSSModeZenithAngle: Parameter[Double] = zenithAngleKey -> 0.0 withUnits zenithAngleUnits

  var cElevation: Parameter[Double]   = initialElevation
  var cFocusError: Parameter[Double]  = initialFocusError
  var cZenithAngle: Parameter[Double] = initialZenithAngle

  override def onMessage(msg: FollowActorMessages): Behavior[FollowActorMessages] = msg match {

    case StopFollowing => this
    case UpdatedEventData(zenithAngleIn, focusErrorIn, time) => {
      if (zenithAngleIn.units != zenithAngleUnits || focusErrorIn.units != focusErrorUnits) {
        println(
          s"Ignoring event data received with improper units: zenithAngle: ${zenithAngleIn.units}, focusError: ${focusErrorIn.units}"
        )
      } else if (!verifyZenithAngle(zenithAngleIn) || !verifyFocusError(calculationConfig, focusErrorIn)) {
        println(s"Ignoring out of range event data: zenithAngle: $zenithAngleIn, focusError: $focusErrorIn")
      } else {
        val totalRangeDistance =
          focusZenithAngleToRangeDistance(calculationConfig, cElevation.head, focusErrorIn.head, zenithAngleIn.head)

        val newElevation = rangeDistanceToElevation(totalRangeDistance, zenithAngleIn.head)

        if (!inNSSMode.head) {
          sendAOESWUpdate(naElevationKey     -> newElevation withUnits naElevationUnits,
                          naRangeDistanceKey -> totalRangeDistance withUnits naRangeDistanceUnits)
        }

        val newTrombonePosition =
          calculateNewTrombonePosition(calculationConfig, cElevation, focusErrorIn, zenithAngleIn)

        sendTrombonePosition(controlConfig, newTrombonePosition)

        sendEngrUpdate(focusErrorIn, newTrombonePosition, zenithAngleIn)

        cFocusError = focusErrorIn
        cZenithAngle = zenithAngleIn
      }
      this
    }
    case SetElevation(elevation) =>
      cElevation = elevation
      ctx.self ! UpdatedEventData(cZenithAngle, cFocusError, EventTime())
      this

    case SetZenithAngle(zenithAngle) =>
      ctx.self ! UpdatedEventData(zenithAngle, cFocusError, EventTime())
      this
  }

  def calculateNewTrombonePosition(calculationConfig: TromboneCalculationConfig,
                                   elevationIn: Parameter[Double],
                                   focusErrorIn: Parameter[Double],
                                   zenithAngleIn: Parameter[Double]): Parameter[Double] = {
    val totalRangeDistance =
      focusZenithAngleToRangeDistance(calculationConfig, elevationIn.head, focusErrorIn.head, zenithAngleIn.head)

    val stagePosition = rangeDistanceToStagePosition(totalRangeDistance)
    spos(stagePosition)
  }

  def sendTrombonePosition(controlConfig: TromboneControlConfig, stagePosition: Parameter[Double]): Unit = {
    tromboneControl.foreach(_ ! GoToStagePosition(stagePosition))
  }

  def sendAOESWUpdate(elevationItem: Parameter[Double], rangeItem: Parameter[Double]): Unit = {
    aoPublisher.foreach(_ ! AOESWUpdate(elevationItem, rangeItem))
  }

  def sendEngrUpdate(focusError: Parameter[Double],
                     trombonePosition: Parameter[Double],
                     zenithAngle: Parameter[Double]): Unit = {
    engPublisher.foreach(_ ! EngrUpdate(focusError, trombonePosition, zenithAngle))
  }
}
