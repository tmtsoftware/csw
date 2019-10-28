package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.CommandComponentState._
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.time.core.models.UTCTime

import scala.concurrent.{ExecutionContext, Future}

class CommandHcdHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._

  private val log: Logger                   = loggerFactory.getLogger(ctx)
  private implicit val ec: ExecutionContext = ctx.executionContext

  override def initialize(): Future[Unit] = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW comp    onents
    log.info("Initializing Component TLA")
    Thread.sleep(100)

    //#currentStatePublisher
    // Publish the CurrentState using parameter set created using a sample Choice parameter
    currentStatePublisher.publish(CurrentState(filterHcdPrefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    //#currentStatePublisher

    Future.unit
  }

  override def onGoOffline(): Unit =
    currentStatePublisher.publish(CurrentState(filterHcdPrefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit =
    currentStatePublisher.publish(CurrentState(filterHcdPrefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))

  def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse = {
    command.commandName match {
      case `immediateCmd`        => Accepted(command.commandName, runId)
      case `longRunningCmdToHcd` => Accepted(command.commandName, runId)
      case `shorterHcdCmd`       => Accepted(command.commandName, runId)
      case `shorterHcdErrorCmd`  => Accepted(command.commandName, runId)
      case `invalidCmd`          => Invalid(command.commandName, runId, OtherIssue("Invalid"))
      case _ =>
        Invalid(command.commandName, runId, OtherIssue("Testing: Received failure, will return Invalid."))
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    // Adding passed in parameter to see if data is transferred properly
    processCommand(runId, controlCommand)
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    processCommand(runId, controlCommand)
  }

  // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
  private def processCommand(runId: Id, command: ControlCommand): SubmitResponse = {
    command.commandName match {
      case `immediateCmd` =>
        Completed(command.commandName, runId)
      case `longRunningCmdToHcd` =>
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(3))) {
          commandResponseManager.updateCommand(Completed(command.commandName, runId))
        }
        // HCD starts long-running command and returns started
        Started(command.commandName, runId)
      case `shorterHcdCmd` =>
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(1))) {
          commandResponseManager.updateCommand(Completed(command.commandName, runId))
        }
        // HCD starts shorter command and returns started
        Started(command.commandName, runId)
      case `shorterHcdErrorCmd` =>
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(1))) {
          commandResponseManager.updateCommand(Error(command.commandName, runId, "ERROR"))
        }
        // HCD starts shorter command and returns started
        Started(command.commandName, runId)
      case _ =>
        Invalid(command.commandName, runId, CommandIssue.UnsupportedCommandIssue(s"${command.commandName.name}"))
    }
  }

  override def onShutdown(): Future[Unit] = Future {
    currentStatePublisher.publish(CurrentState(filterHcdPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(500)
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}
}
