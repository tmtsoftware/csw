package csw.common.components.command

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.common.components.command.ComponentStateForCommand.{longRunningCmdCompleted, _}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.command.messages.TopLevelActorMessage
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.params.commands.{CommandIssue, CommandResponse, ControlCommand, Setup}
import csw.location.api.models.{AkkaLocation, TrackingEvent}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.command.scaladsl.CommandService

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class McsAssemblyComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  implicit val timeout: Timeout     = 10.seconds
  implicit val scheduler: Scheduler = ctx.system.scheduler
  implicit val ec: ExecutionContext = ctx.executionContext
  var hcdComponent: CommandService  = _
  var runId: Id                     = _
  var shortSetup: Setup             = _
  var mediumSetup: Setup            = _
  var longSetup: Setup              = _

  import cswCtx._
  override def initialize(): Future[Unit] =
    componentInfo.connections.headOption match {
      case Some(hcd) ⇒
        cswCtx.locationService.resolve(hcd.of[AkkaLocation], 5.seconds).map {
          case Some(akkaLocation) ⇒ hcdComponent = new CommandService(akkaLocation)(ctx.system)
          case None               ⇒ throw new RuntimeException("Could not resolve hcd location, Initialization failure.")
        }
      case None ⇒ Future.successful(Unit)
    }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = Unit

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    controlCommand.commandName match {
      case `longRunning` ⇒
        //#addOrUpdateCommand
        // after validation of the controlCommand, update its status of successful validation as Accepted
        commandResponseManager.addOrUpdateCommand(controlCommand.runId, Accepted(controlCommand.runId))
        //#addOrUpdateCommand
        Accepted(controlCommand.runId)
      case `moveCmd`    ⇒ Accepted(controlCommand.runId)
      case `initCmd`    ⇒ Accepted(controlCommand.runId)
      case `invalidCmd` ⇒ Invalid(controlCommand.runId, CommandIssue.OtherIssue("Invalid"))
      case _            ⇒ CommandResponse.Error(controlCommand.runId, "")
    }
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    controlCommand.commandName match {
      case `longRunning` ⇒
        runId = controlCommand.runId

        //#addSubCommand
        shortSetup = Setup(prefix, shortRunning, controlCommand.maybeObsId)
        commandResponseManager.addSubCommand(runId, shortSetup.runId)
        //#addSubCommand

        mediumSetup = Setup(prefix, mediumRunning, controlCommand.maybeObsId)
        // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
        commandResponseManager.addSubCommand(runId, mediumSetup.runId)

        longSetup = Setup(prefix, longRunning, controlCommand.maybeObsId)
        commandResponseManager.addSubCommand(runId, longSetup.runId)

        // this is to simulate that assembly is splitting command into three sub commands and forwarding same to hcd
        // longSetup takes 5 seconds to finish
        // shortSetup takes 1 second to finish
        // mediumSetup takes 3 seconds to finish
        processCommand(longSetup)
        processCommand(shortSetup)
        processCommand(mediumSetup)

        // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
        //#subscribe-to-command-response-manager
        // subscribe to the status of original command received and publish the state when its status changes to
        // Completed
        commandResponseManager.subscribe(
          controlCommand.runId, {
            case Completed(_) ⇒
              currentStatePublisher.publish(
                CurrentState(controlCommand.source, StateName("testStateName"), Set(choiceKey.set(longRunningCmdCompleted)))
              )
            case _ ⇒
          }
        )
        //#subscribe-to-command-response-manager

        //#query-command-response-manager
        // query CommandResponseManager to get the current status of Command, for example: Accepted/Completed/Invalid etc.
        commandResponseManager
          .query(controlCommand.runId)
          .map(
            _ ⇒ () // may choose to publish current state to subscribers or do other operations
          )
      //#query-command-response-manager

      case `initCmd` ⇒ commandResponseManager.addOrUpdateCommand(controlCommand.runId, Completed(controlCommand.runId))
      case `moveCmd` ⇒ commandResponseManager.addOrUpdateCommand(controlCommand.runId, Completed(controlCommand.runId))
      case _         ⇒ //do nothing
    }
  }

  private def processCommand(controlCommand: ControlCommand) = {
    hcdComponent
      .submit(controlCommand)
      .map {
        case response: Accepted ⇒
          // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
          commandResponseManager.updateSubCommand(response.runId, Accepted(response.runId))
          //#updateSubCommand
          // An original command is split into sub-commands and sent to a component. The result of the command is
          // obtained by subscribing to the component with the sub command id.
          hcdComponent.subscribe(controlCommand.runId).map {
            case _: Completed ⇒
              controlCommand.runId match {
                case id if id == shortSetup.runId ⇒
                  currentStatePublisher
                    .publish(CurrentState(shortSetup.source, StateName("testStateName"), Set(choiceKey.set(shortCmdCompleted))))
                  // As the commands get completed, the results are updated in the commandResponseManager
                  commandResponseManager.updateSubCommand(id, Completed(id))
                case id if id == mediumSetup.runId ⇒
                  currentStatePublisher
                    .publish(CurrentState(mediumSetup.source, StateName("testStateName"), Set(choiceKey.set(mediumCmdCompleted))))
                  commandResponseManager.updateSubCommand(id, Completed(id))
                case id if id == longSetup.runId ⇒
                  currentStatePublisher
                    .publish(CurrentState(longSetup.source, StateName("testStateName"), Set(choiceKey.set(longCmdCompleted))))
                  commandResponseManager.updateSubCommand(id, Completed(id))
              }
            //#updateSubCommand
            case _ ⇒ // Do nothing
          }
        case _ ⇒ // Do nothing
      }
  }

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = Future.successful(Unit)

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
