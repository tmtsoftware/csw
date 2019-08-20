package csw.common.components.command

import akka.actor
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.TrackingEvent
import csw.params.commands.CommandIssue.{OtherIssue, WrongPrefixIssue}
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, ControlCommand, Result, Setup}
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ComponentHandlerForCommand(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  private val cancelCmdId   = KeyType.StringKey.make("cancelCmdId")
  private val cancelCmdName = KeyType.StringKey.make(name = "cancelCmdName")

  import ComponentStateForCommand._
  private implicit val actorSystem: actor.ActorSystem = ctx.system.toUntyped
  private implicit val ec: ExecutionContext           = ctx.executionContext
  private implicit val mat: ActorMaterializer         = ActorMaterializer()

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse =
    controlCommand.commandName match {
      case `acceptedCmd`          => Accepted(controlCommand.commandName, runId)
      case `longRunningCmd`       => Accepted(controlCommand.commandName, runId)
      case `onewayCmd`            => Accepted(controlCommand.commandName, runId)
      case `matcherCmd`           => Accepted(controlCommand.commandName, runId)
      case `matcherFailedCmd`     => Accepted(controlCommand.commandName, runId)
      case `matcherTimeoutCmd`    => Accepted(controlCommand.commandName, runId)
      case `cancelCmd`            => Accepted(controlCommand.commandName, runId)
      case `assemCurrentStateCmd` => Accepted(controlCommand.commandName, runId)
      case `hcdCurrentStateCmd`   => Accepted(controlCommand.commandName, runId)
      case `immediateCmd`         => Accepted(controlCommand.commandName, runId)
      case `immediateResCmd`      => Accepted(controlCommand.commandName, runId)
      case `invalidCmd` =>
        Invalid(controlCommand.commandName, runId, OtherIssue(s"Unsupported prefix: ${controlCommand.commandName}"))
      case _ => Invalid(controlCommand.commandName, runId, WrongPrefixIssue(s"Wrong prefix: ${controlCommand.commandName}"))
    }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    controlCommand match {
      case s @ Setup(_, `cancelCmd`, _, _) =>
        processAcceptedSubmitCmd(runId, s)
      case s @ Setup(_, `longRunningCmd`, _, _) =>
        processCommandWithoutMatcher(runId, s)
        Started(controlCommand.commandName, runId)
      case s @ Setup(_, `acceptedCmd`, _, _) =>
        // Update so it can be returned when cancelled
        Started(s.commandName, runId)
      case s @ Setup(_, `immediateCmd`, _, _) =>
        Completed(s.commandName, runId)
      case s @ Setup(_, `immediateResCmd`, _, _) =>
        CompletedWithResult(controlCommand.commandName, runId, Result(s.source, Set(KeyType.IntKey.make("encoder").set(20))))
      case c =>
        Error(controlCommand.commandName, runId, s"Some other command received: $c")
    }
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = controlCommand.commandName match {
    case `cancelCmd` =>
      processAcceptedSubmitCmd(runId, controlCommand.asInstanceOf[Setup])
    case `onewayCmd`          => // Do nothing
    case `matcherCmd`         => processCommandWithMatcher(controlCommand)
    case `matcherFailedCmd`   => processCommandWithMatcher(controlCommand)
    case `acceptedCmd`        => //mimic long running process by publishing any state
    case `matcherTimeoutCmd`  => processCommandWithMatcher(controlCommand)
    case `hcdCurrentStateCmd` => processCurrentStateOneway(controlCommand)
    case c                    => println(s"onOneway received an unknown command: $c")
  }

  private def processCurrentStateOneway(controlCommand: ControlCommand): Unit = {
    //#subscribeCurrentState
    val currentState = CurrentState(prefix, StateName("HCDState"), controlCommand.paramSet)
    cswCtx.currentStatePublisher.publish(currentState)
    //#subscribeCurrentState
  }

  private def processAcceptedSubmitCmd(cancelCommandId: Id, cancelCommandSetup: Setup): SubmitResponse = {
    val commandToCancelId: Option[Parameter[String]]   = cancelCommandSetup.get(cancelCmdId)
    val commandToCancelName: Option[Parameter[String]] = cancelCommandSetup.get(cancelCmdName)
    if (commandToCancelId.isEmpty || commandToCancelName.isEmpty)
      Error(
        cancelCommandSetup.commandName,
        cancelCommandId,
        "Cancel command or cancel command name not present in cancel command"
      )
    else {
      processCancelCommand(
        cancelCommandId,
        cancelCommandSetup,
        CommandName(commandToCancelName.get.head),
        Id(commandToCancelId.get.head)
      )
    }
  }

  private def processCancelCommand(
      cancelCommandId: Id,
      cancelCommandSetup: Setup,
      commandToCancelName: CommandName,
      commandToCancelId: Id
  ): SubmitResponse = {
    processOriginalCommand(commandToCancelName, commandToCancelId)
    // This completes the cancel command itself
    Completed(cancelCommandSetup.commandName, cancelCommandId)
  }

  // This simulates the low-level handling of cancelling the original long-running command
  private def processOriginalCommand(commandToCancelName: CommandName, commandToCancelId: Id): Unit = {
    // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
    commandUpdatePublisher.update(Cancelled(commandToCancelName, commandToCancelId))
  }

  // This simulates a long command that has been started and finishes with a result
  private def processCommandWithoutMatcher(runId: Id, controlCommand: ControlCommand): Unit = {
    val param: Parameter[Int] = encoder.set(20)
    val result                = Result(controlCommand.source, Set(param))

    // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
    // This simulates a long running command that eventually updates with a result
    Source
      .fromFuture(Future(CompletedWithResult(controlCommand.commandName, runId, result)))
      .delay(500.milli)
      .runWith(Sink.head)
      .onComplete {
        case Success(sr)        => commandUpdatePublisher.update(sr) // This is replacement for CRM
        case Failure(exception) => println(s"processWithout exception ${exception.getMessage}")
      }
  }

  private def processCommandWithMatcher(controlCommand: ControlCommand): Unit =
    controlCommand.commandName match {
      case `matcherTimeoutCmd` => Thread.sleep(1000)
      case `matcherFailedCmd` =>
        Source(1 to 10)
          .map(
            i =>
              currentStatePublisher.publish(
                CurrentState(controlCommand.source, StateName("testStateName"), Set(KeyType.IntKey.make("encoder").set(i * 1)))
              )
          )
          .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
          .runWith(Sink.ignore)
      case _ =>
        Source(1 to 10)
          .map(
            i =>
              currentStatePublisher.publish(
                CurrentState(controlCommand.source, StateName("testStateName"), Set(KeyType.IntKey.make("encoder").set(i * 10)))
              )
          )
          .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
          .runWith(Sink.ignore)
    }

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
