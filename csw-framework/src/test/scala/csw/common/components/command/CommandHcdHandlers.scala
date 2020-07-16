package csw.common.components.command

import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.CommandComponentState._
import csw.framework.models.{ComponentContext, CswContext}
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.time.core.models.UTCTime

import scala.concurrent.{ExecutionContext, Future}

class CommandHcdHandlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._

  private val log: Logger                   = loggerFactory.getLogger(ctx.self)
  private implicit val ec: ExecutionContext = ctx.executionContext

  override def initialize(): Future[Unit] = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    log.info("Initializing HCD Component TLA")
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
      case `immediateCmd`        => Accepted(runId)
      case `longRunningCmdToHcd` => Accepted(runId)
      case `shorterHcdCmd`       => Accepted(runId)
      case `shorterHcdErrorCmd`  => Accepted(runId)
      case `invalidCmd`          => Invalid(runId, OtherIssue("Invalid"))
      case _                     => Invalid(runId, OtherIssue("Testing: Received failure, will return Invalid."))
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    // Adding passed in parameter to see if data is sdtransferred properly
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
        Completed(runId)
      case `longRunningCmdToHcd` =>
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(3))) {
          commandResponseManager.updateCommand(Completed(runId))
        }
        // HCD starts long-running command and returns started
        Started(runId)
      case `shorterHcdCmd` =>
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(1))) {
          commandResponseManager.updateCommand(Completed(runId))
        }
        // HCD starts shorter command and returns started
        Started(runId)
      case `shorterHcdErrorCmd` =>
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(1))) {
          commandResponseManager.updateCommand(Error(runId, "ERROR"))
        }
        // HCD starts shorter command and returns started
        Started(runId)
      case _ =>
        Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"${command.commandName.name}"))
    }
  }

  override def onShutdown(): Future[Unit] =
    Future {
      currentStatePublisher.publish(CurrentState(filterHcdPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
      Thread.sleep(500)
    }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}
}
