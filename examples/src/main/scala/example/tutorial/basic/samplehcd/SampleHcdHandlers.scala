package example.tutorial.basic.samplehcd

import akka.actor.Cancellable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Id
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import example.tutorial.basic.samplehcd.SleepWorker.Sleep
import example.tutorial.basic.shared.SampleInfo._

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
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
  private implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                                   = loggerFactory.getLogger
  private val prefix: Prefix                        = cswCtx.componentInfo.prefix

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

  //#initialize
  var maybePublishingGenerator: Option[Cancellable] = None

  override def initialize(): Future[Unit] = {
    log.info(s"HCD: $prefix initializing")
    maybePublishingGenerator = Some(publishCounter())
    Future.unit
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"TrackingEvent received: ${trackingEvent.connection.name}")
  }

  override def onShutdown(): Future[Unit] = {
    log.info(s"HCD: $prefix is shutting down")
    Future.unit
  }
  //#initialize

  //#publish
  import scala.concurrent.duration._
  private def publishCounter(): Cancellable = {
    var counter = 0
    def incrementCounterEvent() =
      Option {
        counter += 1
        val param: Parameter[Int] = KeyType.IntKey.make("counter").set(counter)
        SystemEvent(prefix, EventName("HcdCounter")).add(param)
      }

    log.info(s"HCD: $prefix started publishing stream")
    eventService.defaultPublisher.publish(incrementCounterEvent(), 2.second, err => log.error(err.getMessage, ex = err))
  }

  private def stopPublishingGenerator(): Unit = {
    log.info(s"HCD: $prefix stops publishing stream")
    maybePublishingGenerator.foreach(_.cancel)
  }
  //#publish

  //#validate
  override def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse =
    command.commandName match {
      case `hcdSleep` | `hcdImmediate` =>
        Accepted(runId)
      case _ =>
        log.error(s"HCD: $prefix received an unsupported command: ${command.commandName.name}")
        Invalid(runId, UnsupportedCommandIssue(s"Command: ${command.commandName.name} is not supported for HCD: $prefix."))
    }
  //#validate

  //#onSetup
  override def onSubmit(runId: Id, command: ControlCommand): SubmitResponse = {
    log.info(s"HCD: $prefix handling command: ${command.commandName}")

    command match {
      case setup: Setup => onSetup(runId, setup)
      case _ => // implement (or not)
        Invalid(runId, UnsupportedCommandIssue("HCD: $prefix only supports Setup commands"))
    }
  }

  def onSetup(runId: Id, setup: Setup): SubmitResponse = {
    log.info(s"HCD: $prefix onSubmit received command: $setup")

    setup.commandName match {
      case `hcdSleep` =>
        val sleepTime = setup(sleepTimeKey).head
        val worker    = ctx.spawnAnonymous(SleepWorker(cswCtx))
        worker ! Sleep(runId, sleepTime)
        Started(runId)
      case `hcdImmediate` =>
        Completed(runId)
      case other =>
        Invalid(runId, UnsupportedCommandIssue(s"HCD: $prefix does not implement command: $other"))
    }
  }
  //#onSetup
}

//#worker-actor
//#updateCommand
object SleepWorker {
  import example.tutorial.basic.shared.SampleInfo._

  sealed trait SleepWorkerMessages
  case class Sleep(runId: Id, sleepTime: Long)    extends SleepWorkerMessages
  case class Finished(runId: Id, sleepTime: Long) extends SleepWorkerMessages

  def apply(cswContext: CswContext): Behavior[SleepWorkerMessages] =
    Behaviors.receive { (ctx, message) =>
      message match {
        case Sleep(runId, sleepTime) =>
          val when: UTCTime = UTCTime.after(FiniteDuration(sleepTime, MILLISECONDS))
          cswContext.timeServiceScheduler.scheduleOnce(when, ctx.self.toClassic, Finished(runId, sleepTime))
          Behaviors.same
        case Finished(runId, sleepTime) =>
          cswContext.commandResponseManager.updateCommand(Completed(runId, Result().madd(resultKey.set(sleepTime))))
          Behaviors.stopped
      }
    }
}
//#updateCommand
//#worker-actor
