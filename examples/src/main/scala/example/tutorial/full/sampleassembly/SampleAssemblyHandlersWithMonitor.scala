package example.tutorial.full.sampleassembly

import akka.actor.typed.{ActorSystem, Scheduler}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandResponseManager.{OverallFailure, OverallSuccess}
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.event.api.scaladsl.EventSubscription
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.location.api.models.Connection.AkkaConnection
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{ControlCommand, Observe, Result, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.models.Id
import csw.params.events._
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime
import example.tutorial.basic.shared.SampleInfo.{resultKey, shortCommand}
import example.tutorial.full.shared.{SampleValidation, WorkerMonitor}
import example.tutorial.full.shared.WorkerMonitor.{AddWorker, GetWorker, RemoveWorker, Response}
import example.tutorial.moderate.shared.SampleInfo._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/framework.html
 */
class SampleAssemblyHandlersWithMonitor(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  private implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private implicit val system: ActorSystem[Nothing] = ctx.system
  private implicit val timeout: Timeout             = 10.seconds
  private implicit val sched: Scheduler             = ctx.system.scheduler
  private val log                                   = loggerFactory.getLogger
  private val hcdConnection                         = AkkaConnection(ComponentId(Prefix(Subsystem.ESW, "SampleHcd"), ComponentType.HCD))
  private var hcdLocation: AkkaLocation             = _
  private var hcdCS: Option[CommandService]         = None
  private val prefix: Prefix                        = cswCtx.componentInfo.prefix

  //#initialize
  private var maybeEventSubscription: Option[EventSubscription] = None
  override def initialize(): Future[Unit] = {
    log.info(s"Assembly: $prefix initialize")
    maybeEventSubscription = Some(subscribeToHcd())
    Future.unit
  }

  override def onShutdown(): Future[Unit] = {
    log.info(s"Assembly: $prefix is shutting down.")
    Future.unit
  }
  //#initialize

  //#track-location
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"onLocationTrackingEvent called: $trackingEvent")
    trackingEvent match {
      case LocationUpdated(location) =>
        hcdLocation = location.asInstanceOf[AkkaLocation]
        hcdCS = Some(CommandServiceFactory.make(location))
        onSetup(Id(), Setup(prefix, shortCommand, None))
      case LocationRemoved(connection) =>
        if (connection == hcdConnection) {
          hcdCS = None
        }
    }
  }
  //#track-location

  //#subscribe
  private val counterEventKey = EventKey(prefix, EventName("HcdCounter"))
  private val hcdCounterKey   = KeyType.IntKey.make("counter")

  private def processEvent(event: Event): Unit = {
    log.info(s"Assembly: $prefix received event: ${event.eventKey}")
    event match {
      case e: SystemEvent =>
        e.eventKey match {
          case `counterEventKey` =>
            val counter = e(hcdCounterKey).head
            log.info(s"Counter = $counter")
          case _ => log.warn("Unexpected event received.")
        }
      case _: ObserveEvent => log.warn("Unexpected ObserveEvent received.") // not expected
    }
  }

  private def subscribeToHcd(): EventSubscription = {
    log.info("Assembly: $prefix starting subscription.")
    eventService.defaultSubscriber.subscribeCallback(Set(counterEventKey), processEvent)
  }

  private def unsubscribeHcd(): Unit = {
    log.info("Assembly: $prefix stopping subscription.")
    maybeEventSubscription.foreach(_.unsubscribe())
  }
  //#subscribe

  override def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse = {
    SampleValidation.doAssemblyValidation(runId, command)
  }

  override def onSubmit(runId: Id, command: ControlCommand): SubmitResponse =
    command match {
      case s: Setup => onSetup(runId, s)
      case _: Observe =>
        Invalid(runId, UnsupportedCommandIssue("Observe commands not supported"))
    }

  private val workerMonitor = ctx.spawnAnonymous(WorkerMonitor[Id](cswCtx))

  private def onSetup(runId: Id, setup: Setup): SubmitResponse =
    setup.commandName match {
      case `immediateCommand` =>
        // Assembly preforms a calculation or reads state information storing in a result
        Completed(runId, Result().add(resultKey.set(1000L)))

      case `shortCommand` =>
        commandHCD(runId, Setup(prefix, hcdShort, setup.maybeObsId))
        Started(runId)

      case `mediumCommand` =>
        commandHCD(runId, Setup(prefix, hcdMedium, setup.maybeObsId))
        Started(runId)

      case `longCommand` =>
        commandHCD(runId, Setup(prefix, hcdLong, setup.maybeObsId))
        Started(runId)

      case `sleep` =>
        //sleepHCD(runId, setup, setup(sleepTimeKey).head)
        val s = Setup(prefix, hcdSleep, setup.maybeObsId).add(setSleepTime(setup(sleepTimeKey).head))
        commandHCD(runId, s)
        Started(runId)

      case `cancelLongCommand` =>
        val cancelRunId = Id(setup(cancelKey).head)
        log.info(s"Assembly: $prefix received cancel worker: $cancelRunId")

        val workerId: Future[Response[Id]] = workerMonitor.ask(GetWorker(cancelRunId, _))
        workerId.foreach { res =>
          hcdCS match {
            case Some(cs) =>
              val s = Setup(cswCtx.componentInfo.prefix, hcdCancelLong, setup.maybeObsId).add(cancelKey.set(res.response.id))
              cs.submitAndWait(s).map {
                case completed: Completed =>
                  commandResponseManager.updateCommand(completed.copy(runId = runId))
                case other =>
                  log.error(s"Assembly: $prefix - failed to cancel command: $runId with $other")
                  commandResponseManager.updateCommand(other.withRunId(runId))
              }
            case None =>
              commandResponseManager.updateCommand(Error(runId, s"A needed HCD is not available: ${hcdConnection.componentId}"))
          }
        }
        Started(runId)

      case `complexCommand` =>
        val medium = simpleHCD(runId, Setup(prefix, hcdMedium, setup.maybeObsId))
        val long   = simpleHCD(runId, Setup(prefix, hcdLong, setup.maybeObsId))

        commandResponseManager
          .queryFinalAll(medium, long)
          .map {
            case OverallSuccess(_) =>
              // Don't care about individual responses with success
              commandResponseManager.updateCommand(Completed(runId))
            case OverallFailure(responses) =>
              val errors = responses.filter(isNegative)
              commandResponseManager.updateCommand(errors.head.withRunId(runId))
          }
          .recover(ex => commandResponseManager.updateCommand(Error(runId, ex.toString)))

        Started(runId)

      case _ => Invalid(runId, UnsupportedCommandIssue("Unhandled command"))
    }

  private def commandHCD(runId: Id, setup: Setup): Unit =
    hcdCS match {
      case Some(cs) =>
        cs.submit(setup).foreach {
          case started: Started =>
            // Do extra work here after submit before queryFinal
            workerMonitor ! AddWorker(runId, started.runId)
            cs.queryFinal(started.runId).foreach { sr =>
              commandResponseManager.updateCommand(sr.withRunId(runId))
              // Once the command is done, remove it from monitor
              workerMonitor ! RemoveWorker(started.runId)
            }
          case other =>
            commandResponseManager.updateCommand(other.withRunId(runId))
        }

      case None =>
        commandResponseManager.updateCommand(Error(runId, s"A needed HCD is not available: ${hcdConnection.componentId}"))
    }

  //#worker-actor
  private def simpleHCD(runId: Id, setup: Setup): Future[SubmitResponse] =
    hcdCS match {
      case Some(cs) =>
        cs.submitAndWait(setup)
      case None =>
        Future(Error(runId, s"A needed HCD is not available: ${hcdConnection.componentId}"))
    }
  //#worker-actor

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {
    // do something on Diagnostic Mode
  }

  override def onOperationsMode(): Unit = {
    // do something on Operations Mode
  }

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

}
