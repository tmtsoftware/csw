package csw.common.components.command

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.ActorContext
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.api.CommandCompleter
import csw.command.api.CommandCompleter.{Completer, OverallResponse}
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.ComponentStateForCommand.{longRunningCmdCompleted, _}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.{AkkaLocation, TrackingEvent}
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandIssue, CommandName, CommandResponse, ControlCommand, Setup}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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

  override def initialize(): Future[Unit] = {
    componentInfo.connections.headOption match {
      case Some(hcd) ⇒
        cswCtx.locationService.resolve(hcd.of[AkkaLocation], 5.seconds).map {
          case Some(akkaLocation) ⇒ hcdComponent = CommandServiceFactory.make(akkaLocation)(ctx.system)
          case None               ⇒ throw new RuntimeException("Could not resolve hcd location, Initialization failure.")
        }
      case None ⇒ Future.successful(Unit)
    }
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = Unit

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = {
    controlCommand.commandName match {
      case `longRunning` ⇒ Accepted(controlCommand.commandName, runId)
      case `moveCmd`     ⇒ Accepted(controlCommand.commandName, runId)
      case `initCmd`     ⇒ Accepted(controlCommand.commandName, runId)
      case `invalidCmd` ⇒ {
        println("Got invalid")
        Invalid(controlCommand.commandName, runId, CommandIssue.OtherIssue("Invalid"))
      }
      case _ ⇒ Invalid(controlCommand.commandName, runId, UnsupportedCommandIssue(controlCommand.commandName.name))
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    controlCommand.commandName match {
      case `longRunning` ⇒
        // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
        //#addSubCommand
        // When receiving the command, onSubmit adds three subCommands
        shortSetup = Setup(prefix, shortRunning, controlCommand.maybeObsId)
        // commandResponseManager.addSubCommand(runId, shortSetup.runId)       --->  DOESN"T WORK ???

        mediumSetup = Setup(prefix, mediumRunning, controlCommand.maybeObsId)
        //commandResponseManager.addSubCommand(runId, mediumSetup.runId)

        longSetup = Setup(prefix, longRunning, controlCommand.maybeObsId)
        //commandResponseManager.addSubCommand(runId, longSetup.runId)
        //#addSubCommand

        // this is to simulate that assembly is splitting command into three sub commands and forwarding same to hcd
        // longSetup takes 5 seconds to finish
        // shortSetup takes 1 second to finish
        // mediumSetup takes 3 seconds to finish
        //processCommand(longSetup) // THIS DOESN"T WORK JUST GETTING IT TO COMPILE
        //processCommand(shortSetup)
        //processCommand(mediumSetup)
        processLongRunningCommand(runId, controlCommand)

        // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
        //#subscribe-to-command-response-manager
        // query the status of original command received and publish the state when its status changes to
        // Completed
        // TODO FIX ME
        /*
        commandResponseManager
          .queryFinal(runId)
          .foreach {
            case Completed(controlCommand.commandName, _) ⇒
              currentStatePublisher.publish(
                CurrentState(controlCommand.source, StateName("testStateName"), Set(choiceKey.set(longRunningCmdCompleted)))
              )
            case _ ⇒
          }

         */
        //#subscribe-to-command-response-manager

        //#query-command-response-manager
        // query CommandResponseManager to get the current status of Command, for example: Accepted/Completed/Invalid etc.
        // TODO FIX ME
        /*
        commandResponseManager
          .query(runId)
          .map(
            _ ⇒ () // may choose to publish current state to subscribers or do other operations
          )

         */
        // Return response
        Started(controlCommand.commandName, runId)
      //#query-command-response-manager

      case `initCmd` ⇒ Completed(controlCommand.commandName, runId)

      case `moveCmd` ⇒ Completed(controlCommand.commandName, runId)

      case _ ⇒ //do nothing
        Completed(controlCommand.commandName, runId)

    }
  }

  def seqFutures[T, U](items: TraversableOnce[T])(yourfunction: T => Future[U]): Future[List[U]] = {
    items.foldLeft(Future.successful[List[U]](Nil)) { (f, item) =>
      f.flatMap { x =>
        yourfunction(item).map(_ :: x)
      }
    } map (_.reverse)
  }

  private def processLongRunningCommand(runId: Id, controlCommand: ControlCommand): Unit = {

    println("IN PROCESS LONG RUNNING COMMAND")
    // Could be different components, can't actually submit parallel commands to an HCD
    val long   = hcdComponent.submit(longSetup)
    val medium = hcdComponent.submit(mediumSetup)
    val short  = hcdComponent.submit(shortSetup)

    val x = Future.sequence(Set(long, medium, short)).onComplete {
      case Success(sr) =>
        println("Got to here")
        val completer = CommandCompleter.Completer(sr)
        sr.foreach(processCommand2(_, completer))
        completer.waitComplete().onComplete {
          case Success(x) =>
            println("Got Success: " + x)
            currentStatePublisher.publish(
              CurrentState(controlCommand.source, StateName("testStateName"), Set(choiceKey.set(longRunningCmdCompleted)))
            )
            val yy = Completed(controlCommand.commandName, runId)
            println("YY: " + yy)
            commandUpdatePublisher.update(yy)
          case Failure(x) =>
            println("GOt failure")
        }
        println("Done foreach")
      case Failure(ex) => println("Some future failure")
    }

  }

  private def processCommand2(sr: SubmitResponse, completer: Completer): Unit = {
    sr match {
      // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
      //#updateSubCommand
      // An original command is split into sub-commands and sent to a component.
      // The current state publishing is not relevant to the updateSubCommand usage.
      case Started(commandName, runId) ⇒
        commandName match {
          case n if n == shortSetup.commandName ⇒
            hcdComponent.queryFinal(runId).map { sr =>
              currentStatePublisher
                .publish(CurrentState(shortSetup.source, StateName("testStateName"), Set(choiceKey.set(shortCmdCompleted))))
              println(s"Updating short and completer: $sr")
              //commandUpdatePublisher.update(sr)
              completer.update(sr)
            }
          // As the commands get completed, the results are updated in
          // the commandResponseManager

          case n if n == mediumSetup.commandName ⇒
            hcdComponent.queryFinal(runId).map { sr =>
              currentStatePublisher
                .publish(CurrentState(mediumSetup.source, StateName("testStateName"), Set(choiceKey.set(mediumCmdCompleted))))
              println(s"Updating medium: $sr")
              //commandUpdatePublisher.update(sr)
              completer.update(sr)
            }

          case n if n == longSetup.commandName ⇒
            hcdComponent.queryFinal(runId).map { sr =>
              currentStatePublisher
                .publish(CurrentState(longSetup.source, StateName("testStateName"), Set(choiceKey.set(longCmdCompleted))))
              println(s"Updating long: $sr")
              //commandUpdatePublisher.update(sr)
              completer.update(sr)
            }
        }
      //#updateSubCommand
      case _ ⇒ // Do nothing
    }
  }

  private def processCommand(controlCommand: ControlCommand): Unit = {
    hcdComponent
      .submit(controlCommand)
      .map {
        // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
        //#updateSubCommand
        // An original command is split into sub-commands and sent to a component.
        // The current state publishing is not relevant to the updateSubCommand usage.
        case Started(commandName, runId) ⇒
          commandName match {
            case n if n == shortSetup.commandName ⇒
              hcdComponent.queryFinal(runId).map { sr =>
                currentStatePublisher
                  .publish(CurrentState(shortSetup.source, StateName("testStateName"), Set(choiceKey.set(shortCmdCompleted))))
                println(s"Updating xshort: $sr")
                commandUpdatePublisher.update(sr)
              }
            // As the commands get completed, the results are updated in
            // the commandResponseManager

            case n if n == mediumSetup.commandName ⇒
              hcdComponent.queryFinal(runId).map { sr =>
                currentStatePublisher
                  .publish(CurrentState(mediumSetup.source, StateName("testStateName"), Set(choiceKey.set(mediumCmdCompleted))))
                println(s"Updating medium: $sr")
                commandUpdatePublisher.update(sr)
              }

            case n if n == longSetup.commandName ⇒
              hcdComponent.queryFinal(runId).map { sr =>
                currentStatePublisher
                  .publish(CurrentState(longSetup.source, StateName("testStateName"), Set(choiceKey.set(longCmdCompleted))))
                println(s"Updating long: $sr")
                commandUpdatePublisher.update(sr)
              }
          }
        //#updateSubCommand
        case _ ⇒ // Do nothing
      }
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = Future.successful(Unit)

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
