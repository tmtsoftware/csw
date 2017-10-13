package csw.trombone.hcd.actors

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.PubSub.PublisherMessage
import csw.messages._
import csw.messages.ccs.Validation
import csw.messages.ccs.Validations.Valid
import csw.messages.ccs.commands.{Observe, Setup}
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.params.models.Units.encoder
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.trombone.hcd.AxisRequest._
import csw.trombone.hcd.AxisResponse._
import csw.trombone.hcd.TromboneEngineering.{GetAxisConfig, GetAxisStats, GetAxisUpdate, GetAxisUpdateNow}
import csw.trombone.hcd._

import scala.async.Async._
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContextExecutor, Future}

class TromboneHcdBehaviorFactory extends ComponentBehaviorFactory[TromboneMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[TromboneMessage] =
    new TromboneHcdHandlers(ctx, componentInfo, pubSubRef, locationService)
}

class TromboneHcdHandlers(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService
) extends ComponentHandlers[TromboneMessage](ctx, componentInfo, pubSubRef, locationService) {

  implicit val timeout: Timeout             = Timeout(2.seconds)
  implicit val scheduler: Scheduler         = ctx.system.scheduler
  implicit val ec: ExecutionContextExecutor = ctx.executionContext

  var current: AxisUpdate                 = _
  var stats: AxisStatistics               = _
  var tromboneAxis: ActorRef[AxisRequest] = _
  var axisConfig: AxisConfig              = _

  override def initialize(): Future[Unit] = async {
    axisConfig = await(getAxisConfig)
    tromboneAxis = ctx.spawnAnonymous(AxisSimulator.behavior(axisConfig, Some(ctx.self)))
    current = await(tromboneAxis ? InitialState)
    stats = await(tromboneAxis ? GetStatistics)
  }

  override def onShutdown(): Future[Unit] = {
    Future.successful(println("shutdown complete during Running context"))
  }

  override def onGoOffline(): Unit = println("Received running offline")

  override def onGoOnline(): Unit = println("Received running offline")

  override def onSetup(commandMessage: CommandMessage): Validation = {
    val validation = ParamValidation.validateSetup(commandMessage.command.asInstanceOf[Setup])
    if (validation == Valid)
      onSetup(commandMessage.command.asInstanceOf[Setup])
    validation
  }

  override def onObserve(commandMessage: CommandMessage): Validation = {
    ParamValidation.validateObserve(commandMessage.command.asInstanceOf[Observe])
  }

  private def onSetup(sc: Setup): Unit = {
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

  def onDomainMsg(tromboneMsg: TromboneMessage): Unit = tromboneMsg match {
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

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???
}
