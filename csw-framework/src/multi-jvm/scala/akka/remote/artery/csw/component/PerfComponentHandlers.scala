package akka.remote.artery.csw.component

import akka.actor
import akka.remote.artery.csw.perf.CommandMaxThroughputSpec.EndResult
import akka.remote.artery.{RateReporter, TaskRunnerMetrics}
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.ccs.commands.CommandResponse.Completed
import csw.messages.ccs.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, TopLevelActorMessage}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.concurrent.Future

class PerfComponentHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers[TopLevelActorDomainMessage](
      ctx,
      componentInfo,
      commandResponseManager,
      pubSubRef,
      locationService,
      loggerFactory: LoggerFactory
    ) {

  val log: Logger = new LoggerFactory(componentInfo.name).getLogger(ctx)

  private var c                                   = 0L
  private var reporter: RateReporter              = _
  private var payloadSize: Int                    = _
  private var printTaskRunnerMetrics: Boolean     = _
  private val taskRunnerMetrics                   = new TaskRunnerMetrics(ctx.system.toUntyped)
  private var correspondingSender: actor.ActorRef = null // the Actor which send the Start message will also receive the report

  override def initialize(): Future[Unit] = Future.successful(Unit)

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def onDomainMsg(msg: TopLevelActorDomainMessage): Unit = msg match {
    case InitComponentForPerfTest(rep, size, print) ⇒ reporter = rep; payloadSize = size; printTaskRunnerMetrics = print
    case Start(corresponding)                       ⇒ correspondingSender = corresponding; corresponding ! Start
    case _: TestMessage                             ⇒ report()
    case _: Warmup                                  ⇒
    case End ⇒
      if (printTaskRunnerMetrics) taskRunnerMetrics.printHistograms()
      log.info(s"sending end result to $correspondingSender")
      correspondingSender ! EndResult(c)
      ctx.stop(ctx.self)
    case x: Echo ⇒ correspondingSender ! x
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    report()
    Completed(controlCommand.runId)
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = ???

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = Future.successful(Unit)

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???

  def report(): Unit = {
    reporter.onMessage(1, payloadSize)
    c += 1
  }
}
