package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandResponseManager.{OverallFailure, OverallSuccess}
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.CommandComponentState._
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.ComponentType.HCD
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, TrackingEvent}
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class CommandAssemblyHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._

  private val log: Logger                   = loggerFactory.getLogger(ctx)
  private implicit val ec: ExecutionContext = ctx.executionContext
  private implicit val timeout: Timeout     = 15.seconds

  private val filterHCDConnection = AkkaConnection(ComponentId(Prefix(Subsystem.WFOS, "FilterHCD"), HCD))
  // val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(ctx.system, clientMat)

  private val filterHCDLocation    = Await.result(locationService.resolve(filterHCDConnection, 5.seconds), 5.seconds)
  var hcdComponent: CommandService = CommandServiceFactory.make(filterHCDLocation.get)(ctx.system)

  private val longRunning       = Setup(seqPrefix, longRunningCmdToHcd, None)
  private val shortRunning      = Setup(seqPrefix, shorterHcdCmd, None)
  private val shortRunningError = Setup(seqPrefix, shorterHcdErrorCmd, None)

  override def initialize(): Unit = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    log.info("Initializing Assembly Component TLA")

    Thread.sleep(100)
    // #currentStatePublisher
    // Publish the CurrentState using parameter set created using a sample Choice parameter
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    // #currentStatePublisher
  }

  override def onGoOffline(): Unit =
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit =
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))

  def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse = {
    command.commandName match {
      case `immediateCmd` | `longRunningCmd` | `longRunningCmdToAsm` | `longRunningCmdToAsmComp` | `longRunningCmdToAsmInvalid` |
          `longRunningCmdToAsmCActor` | `cmdWithBigParameter` =>
        Accepted(runId)
      case `invalidCmd` =>
        Invalid(runId, OtherIssue("Invalid"))
      case _ =>
        Invalid(runId, OtherIssue("Testing: Received failure, will return Invalid."))
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    processCommand(runId, controlCommand)
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {
    processCommand(runId, controlCommand)
  }

  def processCommand(runId: Id, command: ControlCommand): SubmitResponse =
    command.commandName match {
      case `immediateCmd` =>
        Completed(runId)

      case `longRunningCmd` =>
        // A local assembly command that takes some time returning Started
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(2))) {
          // After time expires, send final update
          commandResponseManager.updateCommand(Completed(runId))
        }
        // Starts long-runing and returns started
        Started(runId)

      // #longRunning
      case `longRunningCmdToAsm` =>
        hcdComponent.submitAndWait(longRunning).foreach {
          case _: Completed =>
            commandResponseManager.updateCommand(Completed(runId))
          case other =>
            // Handle some other response besides Completed
            commandResponseManager.updateCommand(other.withRunId(runId))
        }
        // Assembly starts long-running and returns started
        Started(runId)

      // #longRunning
      // #queryFinalAll
      case `longRunningCmdToAsmComp` =>
        // In this case, assembly does not need to do anything until both commands complete
        // Could wait and return directly if commands are fast, but this is better
        val long    = hcdComponent.submitAndWait(longRunning)
        val shorter = hcdComponent.submitAndWait(shortRunning)

        commandResponseManager.queryFinalAll(long, shorter).onComplete {
          case Success(response) =>
            response match {
              case OverallSuccess(r) =>
                commandResponseManager.updateCommand(Completed(runId))
              case OverallFailure(responses) =>
                commandResponseManager.updateCommand(Error(runId, s"$responses"))
            }
          case Failure(ex) =>
            commandResponseManager.updateCommand(Error(runId, ex.toString))
        }
        Started(runId)
      // #queryFinalAll

      case `longRunningCmdToAsmInvalid` =>
        val long    = hcdComponent.submitAndWait(longRunning)
        val shorter = hcdComponent.submitAndWait(shortRunningError)

        commandResponseManager.queryFinalAll(long, shorter).onComplete {
          case Success(response) =>
            response match {
              case OverallSuccess(_) =>
                commandResponseManager.updateCommand(Completed(runId))
              case OverallFailure(_) =>
                commandResponseManager.updateCommand(Error(runId, s"ERROR"))
            }
          case Failure(ex) =>
            commandResponseManager.updateCommand(Error(runId, ex.toString))
        }
        Started(runId)

      case `cmdWithBigParameter` =>
        Completed(runId, Result(command.paramSet))

      case _ =>
        Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"${command.commandName.name}"))
    }

  override def onShutdown(): Unit = {
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(500)
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}
}
