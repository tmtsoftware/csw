package org.tmt.esw.moderate.samplehcd

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.TrackingEvent
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Id
import csw.params.events.{EventName, SystemEvent}
import csw.time.core.models.UTCTime
import org.tmt.esw.moderate.samplehcd.SleepWorker.{Cancel, Sleep, SleepWorkerMessages}
import org.tmt.esw.moderate.shared.SampleInfo._
import org.tmt.esw.moderate.shared.SampleValidation

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Domain specific logic should be written in below handlers.
 * These handlers get invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd, these will be first validated in the
 * supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/framework.html
 */
class SampleHcdHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger

  // Storage for most recent long command for cancel - not a great implementation, but here to show async possible
  var longWorker: Option[ActorRef[SleepWorkerMessages]] = None

  // Sleep periods in milliseconds for short, medium, and long commands
  private val shortSleepPeriod: Long  = 600L
  private val mediumSleepPeriod: Long = 2000L
  private val longSleepPeriod: Long   = 4000L

  //#worker-actor

  //#worker-actor

  //#initialize
  var maybePublishingGenerator: Option[Cancellable] = None
  override def initialize(): Future[Unit] = {
    log.info("In HCD initialize")
    maybePublishingGenerator = Some(publishCounter())
    Future.unit
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"TrackingEvent received: ${trackingEvent.connection.name}")
  }

  override def onShutdown(): Future[Unit] = {
    log.info("HCD is shutting down")
    Future.unit
  }
  //#initialize

  //#publish
  import scala.concurrent.duration._
  private def publishCounter(): Cancellable = {
    var counter = 0
    def incrementCounterEvent() = Option {
      counter += 1
      val param: Parameter[Int] = KeyType.IntKey.make("counter").set(counter)
      SystemEvent(componentInfo.prefix, EventName("HcdCounter")).add(param)
    }

    log.info("Starting publish stream.")
    eventService.defaultPublisher.publish(incrementCounterEvent(), 2.second, err => log.error(err.getMessage, ex = err))
  }

  private def stopPublishingGenerator(): Unit = {
    log.info("Stopping publish stream")
    maybePublishingGenerator.foreach(_.cancel)
  }
  //#publish

  //#validate
  override def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse = {
    log.info(s"Validating command: ${command.commandName.name}")
    SampleValidation.doHcdValidation(runId, command)
  }

  //#onSetup
  override def onSubmit(runId: Id, command: ControlCommand): SubmitResponse = {
    log.info(s"Handling command: ${command.commandName}")

    command match {
      case setup: Setup => onSetup(runId, setup)
      case _: Observe => // implement (or not)
        Invalid(runId, UnsupportedCommandIssue("Observe commands not supported"))
    }
  }

  def onSetup(runId: Id, setup: Setup): SubmitResponse = {
    log.info(s"HCD onSubmit received command: $setup")
    setup.commandName match {
      case `hcdShort` =>
        val worker = ctx.spawnAnonymous(SleepWorker(cswCtx))
        worker ! Sleep(runId, shortSleepPeriod)
        Started(runId)
      case `hcdMedium` =>
        val worker = ctx.spawnAnonymous(SleepWorker(cswCtx))
        worker ! Sleep(runId, mediumSleepPeriod)
        Started(runId)
      case `hcdLong` =>
        val worker = ctx.spawnAnonymous(SleepWorker(cswCtx))
        // Store the value of the current long worker for potential cancel
        longWorker = Some(worker)
        worker ! Sleep(runId, longSleepPeriod)
        Started(runId)
      case `hcdSleep` =>
        val sleepTime = setup(sleepTimeKey).head
        val worker    = ctx.spawnAnonymous(SleepWorker(cswCtx))
        worker ! Sleep(runId, sleepTime)
        Started(runId)
      case `hcdCancelLong` =>
        val cancelRunId               = Id(setup(cancelKey).head)
        implicit val timeout: Timeout = 10.seconds
        implicit val sched: Scheduler = ctx.system.scheduler
        // Kill long here
        longWorker.map(_ ! Cancel)
        longWorker = None
        Completed(runId)
      case other =>
        Invalid(runId, UnsupportedCommandIssue(s"Sample HCD does not support: $other"))
    }
  }
  //#onSetup

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

}
