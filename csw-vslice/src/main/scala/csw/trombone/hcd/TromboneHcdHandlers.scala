package csw.trombone.hcd

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.AskPattern.Askable
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages._
import csw.messages.ccs.CommandIssue.UnsupportedCommandIssue
import csw.messages.ccs.commands.CommandResponse.Invalid
import csw.messages.ccs.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.models.PubSub
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.models.Units.encoder
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory
import csw.trombone.hcd.AxisRequests.{CancelMove, Datum, Home, Move, _}
import csw.trombone.hcd.AxisResponse._

import scala.async.Async._
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContextExecutor, Future}

//#component-factory
class TromboneHcdBehaviorFactory extends ComponentBehaviorFactory {
  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers =
    new TromboneHcdHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService, loggerFactory)
}
//#component-factory

//#component-handlers-class
class TromboneHcdHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(
      ctx,
      componentInfo,
      commandResponseManager,
      pubSubRef,
      locationService,
      loggerFactory
    ) {
  //#component-handlers-class

  //private state of this component
  implicit val timeout: Timeout             = Timeout(2.seconds)
  implicit val scheduler: Scheduler         = ctx.system.scheduler
  implicit val ec: ExecutionContextExecutor = ctx.executionContext

  var current: AxisUpdate                 = _
  var stats: AxisStatistics               = _
  var tromboneAxis: ActorRef[AxisRequest] = _
  var axisConfig: AxisConfig              = _

  //#initialize-handler
  override def initialize(): Future[Unit] = async {
    // fetch config (preferably from configuration service)
    axisConfig = await(getAxisConfig)

    // create a worker actor which is used by this hcd
    tromboneAxis = ctx.spawnAnonymous(AxisSimulator.behavior(axisConfig))

    // initialise some state by using the worker actor created above
    current = await(tromboneAxis ? InitialState)
    stats = await(tromboneAxis ? GetStatistics)
  }
  //#initialize-handler

  override def onShutdown(): Future[Unit] = {
    Future.successful(println("shutdown complete during Running context"))
  }

  //#onGoOffline-handler
  override def onGoOffline(): Unit = {
    // do something when going offline
  }
  //#onGoOffline-handler

  //#onGoOnline-handler
  override def onGoOnline(): Unit = {
    // do something when going online
  }
  //#onGoOnline-handler

  private def onSetup(sc: Setup): Unit = {
    import csw.trombone.hcd.TromboneHcdState._
    println(s"Trombone process received sc: $sc")

    sc.commandName match {
      case `axisMoveCK`   => tromboneAxis ! Move(sc(positionKey).head, diagFlag = true)
      case `axisDatumCK`  => println("Received Datum"); tromboneAxis ! Datum
      case `axisHomeCK`   => tromboneAxis ! Home
      case `axisCancelCK` => tromboneAxis ! CancelMove

      case x @ (`getAxisConfigCK` | `getAxisUpdateCK` | `getAxisStatsCK`) => onEngMsg(x, None)
      case x @ `getAxisUpdateNowCK`                                       => onEngMsg(x, Some(TestProbe[AxisUpdate]()(ctx.system, TestKitSettings(ctx.system)).ref))

      case x => println(s"Unknown config key $x")
    }
  }

  // #validateCommand-handler
  override def validateCommand(controlCommand: ControlCommand): CommandResponse = controlCommand match {
    case setup: Setup     ⇒ ParamValidation.validateSetup(setup)
    case observe: Observe ⇒ ParamValidation.validateObserve(observe)
    case x                ⇒ Invalid(controlCommand.runId, UnsupportedCommandIssue(s"command $x is not supported by this component."))
  }
  // #validateCommand-handler

  //#onSubmit-handler
  override def onSubmit(controlCommand: ControlCommand): Unit = {
    // process command
    onSetup(controlCommand.asInstanceOf[Setup])
  }
  //#onSubmit-handler

  //#onOneway-handler
  override def onOneway(controlCommand: ControlCommand): Unit = {
    // process command
    onSetup(controlCommand.asInstanceOf[Setup])
  }
  //#onOneway-handler

  def onDomainMsg(tromboneMsg: TromboneMessage): Unit = tromboneMsg match {
//    case x: TromboneEngineering => onEngMsg(x)
    case x: AxisResponse => onAxisResponse(x)
  }

  private def onEngMsg(tromboneEngineeringCK: CommandName, replyTo: Option[ActorRef[AxisUpdate]]): Unit =
    tromboneEngineeringCK match {
      case getAxisStatsCK =>
        tromboneAxis ! GetStatistics(TestProbe[AxisStatistics]()(ctx.system, TestKitSettings(ctx.system)).ref)
      case getAxisUpdateCK    => tromboneAxis ! PublishAxisUpdate
      case getAxisUpdateNowCK => replyTo.foreach(_ ! current)
      case getAxisConfigCK =>
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

  private def getAxisConfig: Future[AxisConfig] = Future(AxisConfig(ConfigFactory.load("tromboneHCDAxisConfig.conf")))

  //#onLocationTrackingEvent-handler
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = trackingEvent match {
    case LocationUpdated(location)   => // do something for the tracked location when it is updated
    case LocationRemoved(connection) => // do something for the tracked location when it is no longer available
  }
  //#onLocationTrackingEvent-handler

}
