package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CompleterActor.{OverallFailure, OverallSuccess}
import csw.command.client.{CommandServiceFactory, Completer}
import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.ComponentStateForCommand.{longRunningCmdCompleted, _}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.{AkkaLocation, TrackingEvent}
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandIssue, ControlCommand, Setup}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class McsAssemblyComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {
  val a: Logger = cswCtx.loggerFactory.getLogger

  private implicit val timeout: Timeout     = 10.seconds
  private implicit val ec: ExecutionContext = ctx.executionContext
  private var hcdComponent: CommandService  = _
  private val assemblyPrefix                = prefix

  import cswCtx._

  override def initialize(): Future[Unit] = {
    componentInfo.connections.headOption match {
      case Some(hcd) =>
        cswCtx.locationService.resolve(hcd.of[AkkaLocation], 5.seconds).map {
          case Some(akkaLocation) => hcdComponent = CommandServiceFactory.make(akkaLocation)(ctx.system)
          case None               => throw new RuntimeException("Could not resolve hcd location, Initialization failure.")
        }
      case None => Future.unit
    }
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ()

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = {
    controlCommand.commandName match {
      case `longRunning` => Accepted(controlCommand.commandName, runId)
      case `moveCmd`     => Accepted(controlCommand.commandName, runId)
      case `initCmd`     => Accepted(controlCommand.commandName, runId)
      case `invalidCmd` =>
        Invalid(controlCommand.commandName, runId, CommandIssue.OtherIssue("Invalid"))
      case _ => Invalid(controlCommand.commandName, runId, UnsupportedCommandIssue(controlCommand.commandName.name))
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    controlCommand.commandName match {
      case `longRunning` =>
        processLongRunningCommand(runId, controlCommand)

        // Return response
        Started(controlCommand.commandName, runId)

      case `initCmd` =>
        Completed(controlCommand.commandName, runId)

      case `moveCmd` =>
        Completed(controlCommand.commandName, runId)

      case _ => // do nothing
        Completed(controlCommand.commandName, runId)
    }
  }

  private def processLongRunningCommand(runId: Id, controlCommand: ControlCommand): Unit = {
    // Could be different components, can't actually submit parallel commands to an HCD
    val shortSetup  = Setup(assemblyPrefix, shortRunning, None)
    val mediumSetup = Setup(assemblyPrefix, mediumRunning, None)
    val longSetup   = Setup(assemblyPrefix, longRunning, None)

    // If doing serially, use submitAll
    val long   = hcdComponent.submit(longSetup)
    val medium = hcdComponent.submit(mediumSetup)
    val short  = hcdComponent.submit(shortSetup)

    // sequence used here to issue commands in parallel
    val responses = Set(long, medium, short)
    val completer = Completer(responses)(ctx)
    // Hand the completer to the handleSubcommandResponse so it can be used to update when subcommands complete
    responses.foreach(resF => resF.foreach(res => handleSubcommandResponse(res, completer)))

    completer.waitComplete().onComplete {
      case Success(overall) =>
        currentStatePublisher.publish(
          CurrentState(controlCommand.source, StateName("testStateName"), Set(choiceKey.set(longRunningCmdCompleted)))
        )
        overall match {
          case OverallSuccess(_) =>
            commandResponseManager.updateCommand(Completed(controlCommand.commandName, runId))
          case OverallFailure(responses) =>
            commandResponseManager.updateCommand(Error(controlCommand.commandName, runId, s"$responses"))
        }
      case Failure(x) =>
        // Lift subcommand timeout to an error
        commandResponseManager.updateCommand(Error(controlCommand.commandName, runId, s"${x.toString}"))
    }

  }

  private def handleSubcommandResponse(startedResponse: SubmitResponse, completer: Completer): Unit = {
    startedResponse match {
      // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
      //#updateSubCommand
      // An original command is split into sub-commands and sent to a component.
      // The current state publishing is not relevant to the updateSubCommand usage.
      // As the commands get completed, the results are updated using the command completer
      case Started(commandName, runId) =>
        commandName match {
          case n if n == shortRunning =>
            hcdComponent.queryFinal(runId).map { sr =>
              currentStatePublisher
                .publish(CurrentState(assemblyPrefix, StateName("testStateName"), Set(choiceKey.set(shortCmdCompleted))))
              completer.update(sr)
            }

          case n if n == mediumRunning =>
            hcdComponent.queryFinal(runId).map { sr =>
              currentStatePublisher
                .publish(CurrentState(assemblyPrefix, StateName("testStateName"), Set(choiceKey.set(mediumCmdCompleted))))
              completer.update(sr)
            }

          case n if n == longRunning =>
            hcdComponent.queryFinal(runId).map { sr =>
              currentStatePublisher
                .publish(CurrentState(assemblyPrefix, StateName("testStateName"), Set(choiceKey.set(longCmdCompleted))))
              completer.update(sr)
            }
        }
      //#updateSubCommand
      case _ => // Do nothing
    }
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Future[Unit] = Future.unit

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}
}
