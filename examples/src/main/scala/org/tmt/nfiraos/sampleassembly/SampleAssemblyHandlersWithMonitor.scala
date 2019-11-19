package org.tmt.nfiraos.sampleassembly

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.event.api.scaladsl.EventSubscription
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.Connection.AkkaConnection
import csw.location.models._
import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue, UnsupportedCommandIssue}
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandIssue, ControlCommand, Observe, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.models.Id
import csw.params.events._
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime
import org.tmt.nfiraos.shared.SampleInfo._
import org.tmt.nfiraos.shared.WorkerMonitor
import org.tmt.nfiraos.shared.WorkerMonitor.{AddWorker, GetWorker, Response}

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
  private val log                                   = loggerFactory.getLogger
  private val hcdConnection                         = AkkaConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "samplehcd"), ComponentType.HCD))
  private var hcdLocation: AkkaLocation             = _
  private var hcdCS: Option[CommandService]         = None

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
        println("HCD CS Created")
        hcdCS = Some(CommandServiceFactory.make(location))
      //commandSender ! SendCommand(hcd)
      case LocationRemoved(_) =>
        log.info("HCD no longer available")
        hcdCS = None
    }
  }
  //#track-location

  //#subscribe
  private val counterEventKey = EventKey(Prefix("nfiraos.samplehcd"), EventName("HcdCounter"))
  private val hcdCounterKey   = KeyType.IntKey.make("counter")

  private def processEvent(event: Event): Unit = {
    log.info(s"Event received: ${event.eventKey}")
    event match {
      case e: SystemEvent =>
        e.eventKey match {
          case `counterEventKey` =>
            val counter = e(hcdCounterKey).head
            log.info(s"Counter = $counter")
          case _ => log.warn("Unexpected event received.")
        }
      case e: ObserveEvent => log.warn("Unexpected ObserveEvent received.") // not expected
    }
  }

  private def subscribeToHcd(): EventSubscription = {
    log.info("Starting subscription.")
    eventService.defaultSubscriber.subscribeCallback(Set(counterEventKey), processEvent)
  }

  private def unsubscribeHcd(): Unit = {
    log.info("Stopping subscription.")
    maybeEventSubscription.foreach(_.unsubscribe())
  }
  //#subscribe

  override def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse = {
    command match {
      case setup: Setup if setup.commandName == sleep =>
        validateSleep(runId, setup)
      /*
      case setup: Setup if setup.commandName == sleepWithWorker =>
        validateSleep(runId, setup)
      case setup: Setup if setup.commandName == cancelWorker =>
        validateCancel(runId, setup)
      case setup: Setup if setup.commandName == sleepToHcd =>
        validateSleep(runId, setup)
       */
      case _ =>
        Invalid(runId, UnsupportedCommandIssue(s"Command: ${command.commandName} is not supported."))
    }
  }

  private def validateSleep(runId: Id, setup: Setup): ValidateCommandResponse =
    if (setup.exists(sleepTimeKey)) {
      val sleepTime: Long = setup(sleepTimeKey).head
      if (sleepTime < maxSleep)
        Accepted(runId)
      else
        Invalid(runId, ParameterValueOutOfRangeIssue("sleepTime must be < 2000"))
    }
    else {
      Invalid(runId, MissingKeyIssue(s"required sleep command key: $sleepTimeKey is missing."))
    }

  private def validateCancel(runId: Id, setup: Setup): ValidateCommandResponse =
    if (setup.exists(cancelKey)) {
      Accepted(runId)
    }
    else {
      Invalid(runId, MissingKeyIssue(s"required cancel command key: $cancelKey is missing."))
    }

  override def onSubmit(runId: Id, command: ControlCommand): SubmitResponse =
    command match {
      case s: Setup => onSetup(runId, s)
      case _: Observe =>
        Invalid(runId, UnsupportedCommandIssue("Observe commands not supported"))
    }

  private val workerMonitor = ctx.spawnAnonymous(WorkerMonitor[Id](cswCtx))

  private def onSetup(runId: Id, setup: Setup): SubmitResponse = {

    setup.commandName match {
      case `sleep` =>
        val delay = FiniteDuration(setup(sleepTimeKey).head, MILLISECONDS)
        timeServiceScheduler.scheduleOnce(UTCTime.after(delay)) {
          println("Times up")
          commandResponseManager.updateCommand(Completed(runId))
        }
        Started(runId)
        /*
      case `sleepWithWorker` =>
        println(s"Received xcommand: $setup")
        hcdCS.foreach { cs =>
          val s = Setup(cswCtx.componentInfo.prefix, sleep, None).copy(paramSet = setup.paramSet)
          println("S: " + s)
          cs.submitAndWait(s).map {
            case c: Completed =>
              println("Assembly received completed")
              commandResponseManager.updateCommand(c.copy(runId = runId))
            case a =>
              println(s"Got an: $a response")
          }
        }
        Started(runId)
      case `cancelWorker` =>
        val cancelRunId = Id(setup(cancelKey).head)
        println(s"Assembly received cancel worker: $cancelRunId")
        implicit val timeout: Timeout = 10.seconds
        //implicit val sched:Scheduler = ctx.system.scheduler
        val workerId: Future[Response[Id]] = workerMonitor.ask(replyTo => GetWorker(cancelRunId, replyTo))
        workerId.map { res =>
          println("Id: " + res.response)
          hcdCS match {
            case Some(cs) =>
              val s = Setup(cswCtx.componentInfo.prefix, cancelWorker, None).add(cancelKey.set(res.response.id))
              println("Assembly setup for cancel HCD: " + s)
              cs.submitAndWait(s).map {
                case completed: Completed =>
                  println("Assembly received cancel completed")
                  commandResponseManager.updateCommand(completed.copy(runId = runId))
                case other =>
                  println(s"Got an: $other response")
                  commandResponseManager.updateCommand(other.withRunId(runId))
              }
            case None =>
              commandResponseManager.updateCommand(Error(runId, s"A needed HCD is not available: ${hcdConnection.componentId}"))
            }
        }
        Started(runId)
      case `sleepToHcd` =>
        println("SleepToHCD")
        hcdCS match {
          case Some(cs) =>
            println("Input setup: " + setup)
            val s = Setup(cswCtx.componentInfo.prefix, sleep, None).copy(paramSet = setup.paramSet)
            println("Input setup for HCD: " + s)
            cs.submit(s).map {
              case started: Started =>
                println("Assembly received started")
                workerMonitor ! AddWorker(runId, started.runId)
                cs.queryFinal(started.runId).foreach { sr =>
                  println(s"Query Final final returned: $sr")
                  commandResponseManager.updateCommand(sr.withRunId(runId))
                }
              case other =>
                other.withRunId(runId)
            }
          case None =>
            commandResponseManager.updateCommand(Error(runId, s"A needed HCD is not available: ${hcdConnection.componentId}"))
        }
         */
        Started(runId)
      case _ =>
        Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"${setup.commandName.name}"))
    }
  }

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
