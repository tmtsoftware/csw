package csw.trombone.assembly.actors

import akka.typed.Behavior
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.params.generics.Parameter
import csw.messages.params.models.Choice
import csw.trombone.assembly.TrombonePublisherMsg._
import csw.trombone.assembly.{AssemblyContext, TrombonePublisherMsg}

object TrombonePublisher {
  def make(assemblyContext: AssemblyContext): Behavior[TrombonePublisherMsg] =
    Actor.mutable(ctx ⇒ new TrombonePublisher(assemblyContext, ctx))
}

class TrombonePublisher(
    assemblyContext: AssemblyContext,
    ctx: ActorContext[TrombonePublisherMsg]
) extends MutableBehavior[TrombonePublisherMsg] {
  import TromboneState._

  override def onMessage(msg: TrombonePublisherMsg): Behavior[TrombonePublisherMsg] = msg match {
    case AOESWUpdate(elevationItem, rangeItem) =>
      publishAOESW(elevationItem, rangeItem)
      this

    case EngrUpdate(rtcFocusError, stagePosition, zenithAngle) =>
      publishEngr(rtcFocusError, stagePosition, zenithAngle)
      this

    case TrombonePublisherMsgE(ts) =>
      publishState(ts)
      this

    case AxisStateUpdate(axisName, position, state, inLowLimit, inHighLimit, inHome) =>
      publishAxisState(axisName, position, state, inLowLimit, inHighLimit, inHome)
      this

    case AxisStatsUpdate(
        axisName,
        datumCount,
        moveCount,
        homeCount,
        limitCount,
        successCount,
        failureCount,
        cancelCount
        ) =>
      publishAxisStats(axisName, datumCount, moveCount, homeCount, limitCount, successCount, failureCount, cancelCount)
      this

  }

  private def publishAOESW(elevationItem: Parameter[Double], rangeItem: Parameter[Double]) = {
//    val se = SystemEvent(aoSystemEventPrefix).madd(elevationItem, rangeItem)
//    eventService.foreach(_.publish(se).onComplete {
//      case Failure(ex) => log.error("TrombonePublisher failed to publish AO system event: $se", ex)
//      case _           =>
//    })
  }

  private def publishEngr(
      rtcFocusError: Parameter[Double],
      stagePosition: Parameter[Double],
      zenithAngle: Parameter[Double]
  ) = {
//    val ste = StatusEvent(engStatusEventPrefix).madd(rtcFocusError, stagePosition, zenithAngle)
//    telemetryService.foreach(_.publish(ste).onComplete {
//      case Failure(ex) => log.error(s"TrombonePublisher failed to publish engr event: $ste", ex)
//      case _           =>
//    })
  }

  private def publishState(ts: TromboneState) = {
    // We can do this for convenience rather than using TromboneStateHandler's stateReceive
//    val ste = StatusEvent(tromboneStateStatusEventPrefix).madd(ts.cmd, ts.move, ts.sodiumLayer, ts.nss)
//    telemetryService.foreach(_.publish(ste).onComplete {
//      case Failure(ex) => log.error(s"TrombonePublisher failed to publish trombone state: $ste", ex)
//      case _           =>
//    })
  }

  private def publishAxisState(
      axisName: Parameter[String],
      position: Parameter[Int],
      state: Parameter[Choice],
      inLowLimit: Parameter[Boolean],
      inHighLimit: Parameter[Boolean],
      inHome: Parameter[Boolean]
  ) = {
//    val ste = StatusEvent(axisStateEventPrefix).madd(axisName, position, state, inLowLimit, inHighLimit, inHome)
//    log.debug(s"Axis state publish of $axisStateEventPrefix: $ste")
//    telemetryService.foreach(_.publish(ste).onComplete {
//      case Failure(ex) => log.error(s"TrombonePublisher failed to publish trombone axis state: $ste", ex)
//      case _           =>
//    })
  }

  def publishAxisStats(
      axisName: Parameter[String],
      datumCount: Parameter[Int],
      moveCount: Parameter[Int],
      homeCount: Parameter[Int],
      limitCount: Parameter[Int],
      successCount: Parameter[Int],
      failureCount: Parameter[Int],
      cancelCount: Parameter[Int]
  ): Unit = {
//    val ste = StatusEvent(axisStatsEventPrefix).madd(axisName,
//                                                     datumCount,
//                                                     moveCount,
//                                                     homeCount,
//                                                     limitCount,
//                                                     successCount,
//                                                     failureCount,
//                                                     cancelCount)
//    log.debug(s"Axis stats publish of $axisStatsEventPrefix: $ste")
//    telemetryService.foreach(_.publish(ste).onComplete {
//      case Failure(ex) => log.error(s"TrombonePublisher failed to publish trombone axis stats: $ste", ex)
//      case _           =>
//    })
  }

}
