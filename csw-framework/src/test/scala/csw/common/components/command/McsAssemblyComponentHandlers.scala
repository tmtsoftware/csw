package csw.common.components.command

import akka.actor.Scheduler
import akka.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.TopLevelActorMessage
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed}
import csw.messages.ccs.commands.{CommandResponse, ControlCommand, Setup}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{AkkaLocation, TrackingEvent}
import csw.messages.params.models.Id
import csw.messages.params.states.CurrentState
import csw.services.ccs.scaladsl.{CommandResponseManager, CommandService}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class McsAssemblyComponentHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory) {

  implicit val timeout: Timeout     = 10.seconds
  implicit val scheduler: Scheduler = ctx.system.scheduler
  implicit val ec: ExecutionContext = ctx.executionContext
  var completedCommands: Int        = 0
  var hcdComponent: CommandService  = _
  var commandId: Id                 = _
  var shortSetup: Setup             = _
  var mediumSetup: Setup            = _
  var longSetup: Setup              = _

  override def initialize(): Future[Unit] =
    componentInfo.connections.headOption match {
      case Some(hcd) ⇒
        locationService.resolve(hcd.of[AkkaLocation], 5.seconds).map {
          case Some(akkaLocation) ⇒ hcdComponent = new CommandService(akkaLocation)(ctx.system)
          case None               ⇒ throw new RuntimeException("Could not resolve hcd location, Initialization failure.")
        }
      case None ⇒ Future.successful(Unit)
    }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = Unit

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    controlCommand.commandName match {
      case `longRunning` ⇒ Accepted(controlCommand.runId)
      case `moveCmd`     ⇒ Accepted(controlCommand.runId)
      case `initCmd`     ⇒ Accepted(controlCommand.runId)
      case _             ⇒ CommandResponse.Error(controlCommand.runId, "")
    }
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    controlCommand.commandName match {
      case `longRunning` ⇒
        commandId = controlCommand.runId
        shortSetup = Setup(prefix, shortRunning, controlCommand.maybeObsId)
        mediumSetup = Setup(prefix, mediumRunning, controlCommand.maybeObsId)
        longSetup = Setup(prefix, longRunning, controlCommand.maybeObsId)

        // this is to simulate that assembly is splitting command into three sub commands and forwarding same to hcd
        // longSetup takes 5 seconds to finish
        // shortSetup takes 1 second to finish
        // mediumSetup takes 3 seconds to finish
        processCommand(longSetup)
        processCommand(shortSetup)
        processCommand(mediumSetup)

      case `initCmd` ⇒ commandResponseManager.addOrUpdateCommand(controlCommand.runId, Completed(controlCommand.runId))
      case `moveCmd` ⇒ commandResponseManager.addOrUpdateCommand(controlCommand.runId, Completed(controlCommand.runId))
      case _         ⇒ //do nothing
    }
  }

  private def processCommand(controlCommand: ControlCommand) = {
    hcdComponent
      .submit(controlCommand)
      .map {
        case _: Accepted ⇒
          hcdComponent.subscribe(controlCommand.runId).map {
            case _: Completed ⇒
              controlCommand.runId match {
                case id if id == shortSetup.runId ⇒
                  currentStatePublisher.publish(CurrentState(shortSetup.source, Set(choiceKey.set(shortCmdCompleted))))
                case id if id == mediumSetup.runId ⇒
                  currentStatePublisher.publish(CurrentState(mediumSetup.source, Set(choiceKey.set(mediumCmdCompleted))))
                case id if id == longSetup.runId ⇒
                  currentStatePublisher.publish(CurrentState(longSetup.source, Set(choiceKey.set(longCmdCompleted))))
              }

              completedCommands += 1
              if (completedCommands == 3) {
                commandResponseManager.addOrUpdateCommand(commandId, Completed(commandId))
                completedCommands = 0
              }
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
