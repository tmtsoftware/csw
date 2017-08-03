package csw.trombone.hcd.actors

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models._
import csw.common.framework.scaladsl.hcd.{HcdBehaviorFactory, HcdHandlers}
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState
import csw.param.UnitsOfMeasure.encoder
import csw.trombone.hcd.AxisRequest._
import csw.trombone.hcd.AxisResponse._
import csw.trombone.hcd.TromboneEngineering.{GetAxisConfig, GetAxisStats, GetAxisUpdate, GetAxisUpdateNow}
import csw.trombone.hcd._

import scala.async.Async._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationLong

class TromboneHcdBehaviorFactory extends HcdBehaviorFactory[TromboneMsg] {
  override def make(ctx: ActorContext[ComponentMsg], hcdInfo: HcdInfo): HcdHandlers[TromboneMsg] =
    new TromboneHcdHandlers(ctx, hcdInfo)
}

class TromboneHcdHandlers(ctx: ActorContext[ComponentMsg], hcdInfo: HcdInfo)
    extends HcdHandlers[TromboneMsg](ctx, hcdInfo) {

  implicit val timeout                      = Timeout(2.seconds)
  implicit val scheduler: Scheduler         = ctx.system.scheduler
  implicit val ec: ExecutionContextExecutor = ctx.executionContext

  var current: AxisUpdate                       = _
  var stats: AxisStatistics                     = _
  var tromboneAxis: ActorRef[AxisRequest]       = _
  var axisConfig: AxisConfig                    = _
  var pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.system.deadLetters

  override def initialize(): Future[Unit] = async {
    axisConfig = await(getAxisConfig)
    tromboneAxis = ctx.spawnAnonymous(AxisSimulator.behavior(axisConfig, Some(ctx.self)))
    current = await(tromboneAxis ? InitialState)
    stats = await(tromboneAxis ? GetStatistics)
  }

  override def onRun(): Unit = println("received Running")

  override def onShutdown(): Unit = println("shutdown complete during Running context")

  override def onRestart(): Unit = println("Received do restart")

  override def onGoOffline(): Unit = println("Received running offline")

  override def onGoOnline(): Unit = println("Received running offline")

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
    case GetAxisStats              => tromboneAxis ! GetStatistics(ctx.self)
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
