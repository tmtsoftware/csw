package csw.common.components.command

import akka.actor
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.ThrottleMode
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandIssue.{OtherIssue, WrongPrefixIssue}
import csw.params.commands.CommandResponse._
import csw.params.commands.{ControlCommand, Result, Setup}
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.time.core.models.UTCTime

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ComponentHandlerForCommand(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  private val cancelCmdId = KeyType.StringKey.make("cancelCmdId")

  import ComponentStateForCommand._
  private implicit val actorSystem: actor.ActorSystem = ctx.system.toClassic
  private implicit val ec: ExecutionContext           = ctx.executionContext

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse =
    controlCommand.commandName match {
      case `acceptedCmd`          => Accepted(runId)
      case `longRunningCmd`       => Accepted(runId)
      case `onewayCmd`            => Accepted(runId)
      case `matcherCmd`           => Accepted(runId)
      case `matcherFailedCmd`     => Accepted(runId)
      case `matcherTimeoutCmd`    => Accepted(runId)
      case `cancelCmd`            => Accepted(runId)
      case `assemCurrentStateCmd` => Accepted(runId)
      case `hcdCurrentStateCmd`   => Accepted(runId)
      case `immediateCmd`         => Accepted(runId)
      case `immediateResCmd`      => Accepted(runId)
      case `invalidCmd`           => Invalid(runId, OtherIssue(s"Unsupported prefix: ${controlCommand.commandName}"))
      case _                      => Invalid(runId, WrongPrefixIssue(s"Wrong prefix: ${controlCommand.commandName}"))
    }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    controlCommand match {
      case s @ Setup(_, `cancelCmd`, _, _) =>
        processAcceptedSubmitCmd(runId, s)
      case s @ Setup(_, `longRunningCmd`, _, _) =>
        processCommandWithoutMatcher(runId, s)
        Started(runId)
      case s @ Setup(_, `acceptedCmd`, _, _) =>
        // Update so it can be returned when cancelled
        Started(runId)
      case s @ Setup(_, `immediateCmd`, _, _) =>
        Completed(runId)
      case s @ Setup(_, `immediateResCmd`, _, _) =>
        Completed(runId, Result(KeyType.IntKey.make("encoder").set(20)))
      case c =>
        Error(runId, s"Some other command received: $c")
    }
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = controlCommand.commandName match {
    case `cancelCmd`          => processAcceptedSubmitCmd(runId, controlCommand.asInstanceOf[Setup])
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
    val commandToCancelId: Option[Parameter[String]] = cancelCommandSetup.get(cancelCmdId)
    if (commandToCancelId.isEmpty)
      Error(cancelCommandId, "Cancel command not present in cancel command")
    else {
      processCancelCommand(cancelCommandId, Id(commandToCancelId.get.head))
    }
  }

  private def processCancelCommand(cancelCommandId: Id, commandToCancelId: Id): SubmitResponse = {
    processOriginalCommand(commandToCancelId)
    // This completes the cancel command itself
    Completed(cancelCommandId)
  }

  // This simulates the low-level handling of cancelling the original long-running command
  private def processOriginalCommand(commandToCancelId: Id): Unit = {
    // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
    commandResponseManager.updateCommand(Cancelled(commandToCancelId))
  }

  // This simulates a long command that has been started and finishes with a result
  private def processCommandWithoutMatcher(runId: Id, controlCommand: ControlCommand): Unit = {
    val param: Parameter[Int] = encoder.set(20)
    val result                = Result(param)

    // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
    // This simulates a long running command that eventually updates with a result
    Source
      .future(Future(Completed(runId, result)))
      .delay(250.milli)
      .runWith(Sink.head)
      .onComplete {
        case Success(sr)        => commandResponseManager.updateCommand(sr)
        case Failure(exception) => println(s"processWithout exception ${exception.getMessage}")
      }
  }

  private def processCommandWithMatcher(controlCommand: ControlCommand): Unit =
    controlCommand.commandName match {
      case `matcherTimeoutCmd` => Thread.sleep(1000)
      case `matcherFailedCmd` =>
        Source(1 to 10)
          .map(i =>
            currentStatePublisher.publish(
              CurrentState(controlCommand.source, StateName("testStateName"), Set(KeyType.IntKey.make("encoder").set(i * 1)))
            )
          )
          .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
          .runWith(Sink.ignore)
      case _ =>
        Source(1 to 10)
          .map(i =>
            currentStatePublisher.publish(
              CurrentState(controlCommand.source, StateName("testStateName"), Set(KeyType.IntKey.make("encoder").set(i * 10)))
            )
          )
          .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
          .runWith(Sink.ignore)
    }

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = ???

  override def onOperationsMode(): Unit = ???

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
