package csw.trombone.hcd.actors

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.common.framework.models.ToComponentLifecycleMessage.{
  LifecycleFailureInfo,
  Restart,
  Run,
  RunOffline,
  Shutdown
}
import csw.common.framework.models._
import csw.common.framework.scaladsl._
import csw.param.Parameters.Setup
import csw.param.UnitsOfMeasure.encoder
import csw.trombone.hcd.AxisRequest._
import csw.trombone.hcd.AxisResponse._
import csw.trombone.hcd.TromboneEngineering.{GetAxisConfig, GetAxisStats, GetAxisUpdate, GetAxisUpdateNow}
import csw.trombone.hcd._

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class TromboneHcdFactory extends HcdActorFactory[TromboneMsg] {
  override def make(ctx: ActorContext[HcdMsg],
                    supervisor: ActorRef[HcdComponentLifecycleMessage]): HcdActor[TromboneMsg] =
    new TromboneHcd(ctx, supervisor)
}

class TromboneHcd(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdComponentLifecycleMessage])
    extends HcdActor[TromboneMsg](ctx, supervisor) {

  implicit val timeout              = Timeout(2.seconds)
  implicit val scheduler: Scheduler = ctx.system.scheduler

  var current: AxisUpdate                 = _
  var stats: AxisStatistics               = _
  var tromboneAxis: ActorRef[AxisRequest] = _
  var axisConfig: AxisConfig              = _

  override def initialize(): Future[Unit] = async {
    axisConfig = await(getAxisConfig)
    tromboneAxis = ctx.spawnAnonymous(AxisSimulator.behaviour(axisConfig, Some(domainAdapter)))
    current = await(tromboneAxis ? InitialState)
    stats = await(tromboneAxis ? GetStatistics)
  }

  override def onInitialRun(): Unit = println("received Running")

  override def onInitialHcdShutdownComplete(): Unit = println("received Shutdown complete during Initial context")

  override def onRunningHcdShutdownComplete(): Unit = println("received Shutdown complete during Initial state")

  def onLifecycle(x: ToComponentLifecycleMessage): Unit = x match {
    case Shutdown =>
      println("Received doshutdown")
      supervisor ! ShutdownComplete
    case Restart                             => println("Received do restart")
    case Run                                 => println("Received running")
    case RunOffline                          => println("Received running offline")
    case LifecycleFailureInfo(state, reason) => println(s"Received failed state: $state for reason: $reason")
    case ShutdownComplete                    => println("shutdown complete during Running context")
  }

  def onSetup(sc: Setup): Unit = {
    import csw.trombone.hcd.TromboneHcdState._
    println(s"Trombone process received sc: $sc")

    sc.prefix match {
      case `axisMoveCK` =>
        tromboneAxis ! Move(sc(positionKey).head, diagFlag = true)
      case `axisDatumCK` =>
        println("Received Datum")
        tromboneAxis ! Datum
      case `axisHomeCK` =>
        tromboneAxis ! Home
      case `axisCancelCK` =>
        tromboneAxis ! CancelMove
      case x => println(s"Unknown config key $x")
    }
  }

  def onDomainMsg(tromboneMsg: TromboneMsg): Unit = tromboneMsg match {
    case x: TromboneEngineering => onEngMsg(x)
    case x: AxisResponse        => onAxisResponse(x)
  }

  private def onEngMsg(tromboneEngineering: TromboneEngineering): Unit = tromboneEngineering match {
    case GetAxisStats              => tromboneAxis ! GetStatistics(domainAdapter)
    case GetAxisUpdate             => tromboneAxis ! PublishAxisUpdate
    case GetAxisUpdateNow(replyTo) => replyTo ! current
    case GetAxisConfig =>
      import csw.trombone.hcd.TromboneHcdState._
      val axisConfigState = defaultConfigState.madd(
        lowLimitKey    -> axisConfig.lowLimit,
        lowUserKey     -> axisConfig.lowUser,
        highUserKey    -> axisConfig.highUser,
        highLimitKey   -> axisConfig.highLimit,
        homeValueKey   -> axisConfig.home,
        startValueKey  -> axisConfig.startPosition,
        stepDelayMSKey -> axisConfig.stepDelayMS
      )
      pubSubRef ! PubSub.Publish(axisConfigState)
  }

  private def onAxisResponse(axisResponse: AxisResponse): Unit = axisResponse match {
    case AxisStarted          =>
    case AxisFinished(newRef) =>
    case au @ AxisUpdate(axisName, axisState, current1, inLowLimit, inHighLimit, inHomed) =>
      import csw.trombone.hcd.TromboneHcdState._
      val tromboneAxisState = defaultAxisState.madd(
        positionKey    -> current1 withUnits encoder,
        stateKey       -> axisState.toString,
        inLowLimitKey  -> inLowLimit,
        inHighLimitKey -> inHighLimit,
        inHomeKey      -> inHomed
      )
      pubSubRef ! PubSub.Publish(tromboneAxisState)
      current = au
    case AxisFailure(reason) =>
    case as: AxisStatistics =>
      import csw.trombone.hcd.TromboneHcdState._
      val tromboneStats = defaultStatsState.madd(
        datumCountKey   -> as.initCount,
        moveCountKey    -> as.moveCount,
        limitCountKey   -> as.limitCount,
        homeCountKey    -> as.homeCount,
        successCountKey -> as.successCount,
        failureCountKey -> as.failureCount,
        cancelCountKey  -> as.cancelCount
      )
      pubSubRef ! PubSub.Publish(tromboneStats)
      stats = as
  }

  private def getAxisConfig: Future[AxisConfig] = ???
}
