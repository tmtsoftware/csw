package example.tutorial.basic.sampleassembly

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.util.Timeout
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.event.api.scaladsl.EventSubscription
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.{AkkaLocation, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandResponse, ControlCommand, Setup}
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.{Id, ObsId, Units}
import csw.params.events._
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import example.tutorial.basic.shared.SampleInfo._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/framework.html
 */
//noinspection ScalaStyle
class SampleAssemblyHandlersAlarm(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger

  //#worker-actor
  sealed trait WorkerCommand
  case class SendCommand(hcd: CommandService) extends WorkerCommand

  private val commandSender =
    ctx.spawn(
      Behaviors.receiveMessage[WorkerCommand](msg => {
        msg match {
          case command: SendCommand =>
            log.trace(s"WorkerActor received SendCommand message.")
            handle(command.hcd)
          case _ => log.error("Unsupported message type")
        }
        Behaviors.same
      }),
      "CommandSender"
    )

  private implicit val submitTimeout: Timeout = Timeout(1000.millis)
  def handle(hcd: CommandService): Unit = {

    // Construct Setup command
    val sleepTimeParam: Parameter[Long] = sleepTimeKey.set(5000).withUnits(Units.millisecond)
    val setupCommand                    = Setup(componentInfo.prefix, hcdSleep, Some(ObsId("2018A-001"))).add(sleepTimeParam)

    // Submit command, and handle validation response. Final response is returned as a Future
    val submitCommandResponseF: Future[SubmitResponse] = hcd.submitAndWait(setupCommand).flatMap {
      case x @ (Invalid(_, _) | Locked(_)) =>
        log.error("Sleep command invalid")
        Future(x)
      case x =>
        Future.successful(x)
    }

    // Wait for final response, and log result
    submitCommandResponseF.foreach {
      case _: CommandResponse.Completed => log.info("Command completed successfully")
      case x: CommandResponse.Error     => log.error(s"Command Completed with error: ${x.message}")
      case _                            => log.error("Command failed")
    }
  }
  //#worker-actor

  //#initialize
  private var maybeEventSubscription: Option[EventSubscription] = None
  override def initialize(): Future[Unit] = {
    log.info("In Assembly initialize")
    maybeEventSubscription = Some(subscribeToHcd())
    Future.unit
  }

  override def onShutdown(): Future[Unit] = {
    log.info("Assembly is shutting down.")
    Future.unit
  }
  //#initialize

  //#track-location
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"onLocationTrackingEvent called: $trackingEvent")
    trackingEvent match {
      case LocationUpdated(location) =>
        val hcd = CommandServiceFactory.make(location.asInstanceOf[AkkaLocation])(ctx.system)
        commandSender ! SendCommand(hcd)
      case LocationRemoved(_) => log.info("HCD no longer available")
    }
  }
  //#track-location

  private val counterEventKey = EventKey(Prefix("CSW.samplehcd"), EventName("HcdCounter"))
  private val hcdCounterKey   = KeyType.IntKey.make("counter")

  //#subscribe
  private def processEvent(event: Event): Unit = {
    log.info(s"Event received: ${event.eventKey}")
    event match {
      case e: SystemEvent =>
        e.eventKey match {
          case `counterEventKey` =>
            val counter = e(hcdCounterKey).head
            log.info(s"Counter = $counter")
            setCounterAlarm(counter)

          case _ => log.warn("Unexpected event received.")
        }
      case _: ObserveEvent => log.warn("Unexpected ObserveEvent received.") // not expected
    }
  }
  //#subscribe

  //#alarm
  private val safeRange  = 0 to 10
  private val warnRange  = 11 to 15
  private val majorRange = 16 to 20
  private def getCounterSeverity(counter: Int) = counter match {
    case x if safeRange contains x  => AlarmSeverity.Okay
    case x if warnRange contains x  => AlarmSeverity.Warning
    case x if majorRange contains x => AlarmSeverity.Major
    case _                          => AlarmSeverity.Critical
  }

  private val counterAlarmKey = AlarmKey(componentInfo.prefix, "CounterTooHighAlarm")
  private def setCounterAlarm(counter: Int): Unit = {
    // fire alarm according to counter value
    val severity = getCounterSeverity(counter)
    alarmService.setSeverity(counterAlarmKey, severity).onComplete {
      case Success(_)  => log.info(s"Severity for alarm ${counterAlarmKey.name} set to " + severity.toString)
      case Failure(ex) => log.error(s"Error setting severity for alarm ${counterAlarmKey.name}: ${ex.getMessage}")
    }
  }
  //#alarm

  private def subscribeToHcd(): EventSubscription = {
    log.info("Starting subscription.")
    eventService.defaultSubscriber.subscribeCallback(Set(counterEventKey), processEvent)
  }

  //noinspection ScalaUnusedSymbol
  private def unsubscribeHcd(): Unit = {
    log.info("Stopping subscription.")
    maybeEventSubscription.foreach(_.unsubscribe())
  }

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = Accepted(runId)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = Completed(runId)

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

}
