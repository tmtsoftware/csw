package org.tmt.nfiraos.sampleassembly

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.event.api.scaladsl.EventSubscription
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.location.models.{AkkaLocation, ComponentId, ComponentType, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandIssue, ControlCommand, Observe, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.models.Id
import csw.params.events._
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue, UnsupportedCommandIssue}
import org.tmt.nfiraos.shared.SampleInfo._
import org.tmt.nfiraos.shared.WorkerMonitor
import org.tmt.nfiraos.shared.WorkerMonitor.{AddWorker, GetWorker, Info, Response}
import akka.actor.typed.scaladsl.AskPattern._
import csw.command.client.CommandResponseManager.{OverallFailure, OverallSuccess}
import csw.command.client.extensions.AkkaLocationExt
import csw.location.models.Connection.AkkaConnection

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

private class SampleAssemblyBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new SampleAssemblyHandlers(ctx, cswCtx)
}

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/framework.html
 */
class SampleAssemblyHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  private implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private implicit val system                       = ctx.system
  private implicit val timeout: Timeout             = 10.seconds
  private val log                                   = loggerFactory.getLogger
  private val prefix:Prefix                         = cswCtx.componentInfo.prefix
  private val hcdConnection   = AkkaConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "samplehcd"), ComponentType.HCD))
  private var hcdLocation:AkkaLocation              = _
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
        hcdLocation = location.asInstanceOf[AkkaLocation]
        hcdCS = Some(CommandServiceFactory.make(location))
      //commandSender ! SendCommand(hcd)
      case LocationRemoved(connection) =>
        if (connection == hcdConnection) {
          log.info("HCD no longer available")
          //hcdLocation =
          hcdCS = None
        }
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

  override def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse =
    SampleValidation.doAssemblyValidation(runId, command)

  override def onSubmit(runId: Id, command: ControlCommand): SubmitResponse =
    command match {
      case s: Setup => onSetup(runId, s)
      case _: Observe =>
        Invalid(runId, UnsupportedCommandIssue("Observe commands not supported"))
    }

  private def onSetup(runId: Id, setup: Setup): SubmitResponse = {

    setup.commandName match {
      case `immediateCommand` =>
        Completed(runId)
      case `shortCommand` =>
        commandHCD(runId, Setup(prefix, hcdShort, setup.maybeObsId))
        Started(runId)
      case `mediumCommand` =>
        commandHCD(runId, Setup(prefix, hcdMedium, setup.maybeObsId))
        Started(runId)
      case `longCommand` =>
        commandHCD(runId, Setup(prefix, hcdLong, setup.maybeObsId))
        Started(runId)
      case `cancelLongCommand` =>
        commandHCD(runId, Setup(prefix, hcdCancelLong, setup.maybeObsId).copy(paramSet = setup.paramSet))
        Started(runId)
      case `complexCommand` =>
        val medium = simpleHCD(runId, Setup(prefix, hcdMedium, setup.maybeObsId))
        val long = simpleHCD(runId, Setup(prefix, hcdLong, setup.maybeObsId))

        commandResponseManager.queryFinalAll(medium, long).map {
          case OverallSuccess(_) =>
            // Don't care about individual responses with success
            commandResponseManager.updateCommand(Completed(runId))
          case OverallFailure(responses) =>
            val errors = responses.filter(isNegative(_))
            commandResponseManager.updateCommand(errors.head.withRunId(runId))
        }.recover(ex => commandResponseManager.updateCommand(Error(runId, ex.toString)))

        Started(runId)
      case `sendToLocked` =>
        Completed(runId)
      case `sleep` =>
        sleepHCD(runId, setup, setup(sleepTimeKey).head)
        Started(runId)
      case _ =>
        Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"${setup.commandName.name}"))
    }
  }

  private def sleepHCD(runId: Id, setup:Setup, sleepTime:Long): Unit = {
    println("sleepHCD")
    hcdCS match {
      case Some(cs) =>
        println("Input setup: " + setup)
        val s = Setup(prefix, hcdSleep, None).add(setSleepTime(sleepTime))
        println("Input setup for HCD: " + s)
        cs.submit(s).map {
          case started: Started =>
            println("Assembly received started")
            cs.queryFinal(started.runId).foreach { sr =>
              println(s"Query Final final returned: $sr")
              commandResponseManager.updateCommand(sr.withRunId(runId))
            }
          case other =>
            commandResponseManager.updateCommand(other.withRunId(runId))
          }
      case None =>
        commandResponseManager.updateCommand(Error(runId, s"A needed HCD is not available: ${hcdConnection.componentId}"))
    }
  }

  private def commandHCD(runId: Id, setup:Setup): Unit = {
    println("commandHCD")
    hcdCS match {
      case Some(cs) =>
        println("Input setup: " + setup)
        cs.submitAndWait(setup).map { sr =>
          commandResponseManager.updateCommand(sr.withRunId(runId))
        }
      case None =>
        commandResponseManager.updateCommand(Error(runId, s"A needed HCD is not available: ${hcdConnection.componentId}"))
    }
  }

  private def simpleHCD(runId: Id, setup:Setup):Future[SubmitResponse] = {
    println("SimpleHCD")
    hcdCS match {
      case Some(cs) =>
        cs.submitAndWait(setup)
      case None =>
        Future(Error(runId, s"A needed HCD is not available: ${hcdConnection.componentId}"))
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
