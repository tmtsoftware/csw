package csw.common.components.command

import akka.actor
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.util.Timeout
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.command.client.messages.TopLevelActorMessage
import csw.params.commands.CommandIssue.{OtherIssue, WrongPrefixIssue}
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.location.api.models._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ComponentHandlerForCommand(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  private val cancelCmdId = KeyType.StringKey.make("cancelCmdId")

  import ComponentStateForCommand._
  implicit val actorSystem: actor.ActorSystem = ctx.system.toUntyped
  implicit val ec: ExecutionContext           = ctx.executionContext
  implicit val mat: ActorMaterializer         = ActorMaterializer()

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): ValidateCommandResponse = controlCommand.commandName match {
    case `acceptedCmd`       ⇒ Accepted(controlCommand.runId)
    case `withoutMatcherCmd` ⇒ Accepted(controlCommand.runId)
    case `matcherCmd`        ⇒ Accepted(controlCommand.runId)
    case `matcherFailedCmd`  ⇒ Accepted(controlCommand.runId)
    case `matcherTimeoutCmd` ⇒ Accepted(controlCommand.runId)
    case `cancelCmd`         ⇒ Accepted(controlCommand.runId)
    case `immediateCmd`      ⇒ Accepted(controlCommand.runId)
    case `immediateResCmd`   ⇒ Accepted(controlCommand.runId)
    case `invalidCmd` ⇒
      Invalid(controlCommand.runId, OtherIssue(s"Unsupported prefix: ${controlCommand.commandName}"))
    case _ ⇒ Invalid(controlCommand.runId, WrongPrefixIssue(s"Wrong prefix: ${controlCommand.commandName}"))
  }

  override def onSubmit(controlCommand: ControlCommand): SubmitResponse = controlCommand.commandName match {
    case `cancelCmd` ⇒ processAcceptedSubmitCmd(controlCommand)
    case `withoutMatcherCmd` ⇒
      processCommandWithoutMatcher(controlCommand)
      Started(controlCommand.runId)
    case `acceptedCmd`  ⇒ Started(controlCommand.runId)
    case `immediateCmd` ⇒ Completed(controlCommand.runId)
    case `immediateResCmd` ⇒
      CompletedWithResult(controlCommand.runId, Result(controlCommand.source, Set(KeyType.IntKey.make("encoder").set(20))))
    case c ⇒
      Error(controlCommand.runId, s"Some other command received: $c")
  }

  override def onOneway(controlCommand: ControlCommand): Unit = controlCommand.commandName match {
    case `cancelCmd`         ⇒ processAcceptedOnewayCmd(controlCommand)
    case `matcherCmd`        ⇒ processCommandWithMatcher(controlCommand)
    case `matcherFailedCmd`  ⇒ processCommandWithMatcher(controlCommand)
    case `acceptedCmd`       ⇒ //mimic long running process by publishing any state
    case `matcherTimeoutCmd` => processCommandWithMatcher(controlCommand)
    case c                   ⇒ println(s"onOneway received an unknown command: $c")

  }

  private def processAcceptedSubmitCmd(controlCommand: ControlCommand): SubmitResponse = {
    controlCommand.paramType.get(cancelCmdId) match {
      case None        => Error(controlCommand.runId, "Cancel command not present")
      case Some(param) => processCancelCommand(controlCommand.runId, Id(param.head))
    }
  }

  private def processAcceptedOnewayCmd(controlCommand: ControlCommand): Unit =
    controlCommand.paramType.get(cancelCmdId).foreach(param ⇒ processOriginalCommand(Id(param.head)))

  // This simulates a long command that has been started and finishes with a result
  private def processCommandWithoutMatcher(controlCommand: ControlCommand): Unit = {
    val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(20)
    val result                = Result(controlCommand.source, Set(param))

    // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
    // This simulates a long running command that eventually updates with a result
    Source.fromFuture(Future(CompletedWithResult(controlCommand.runId, result))).delay(250.milli).runWith(Sink.head).onComplete {
      case Success(sr)        => commandResponseManager.addOrUpdateCommand(controlCommand.runId, sr)
      case Failure(exception) => println(s"processWithout exception ${exception.getMessage}")
    }
  }

  private def processCancelCommand(runId: Id, cancelId: Id): SubmitResponse = {
    processOriginalCommand(cancelId)
    Completed(runId)
  }

  // This simulates the handling of cancelling a long command
  private def processOriginalCommand(runId: Id): Unit = {
    implicit val timeout: Timeout = 5.seconds

    // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
    val eventualResponse: Future[QueryResponse] = commandResponseManager.query(runId)
    eventualResponse.onComplete {
      case Success(x) => commandResponseManager.addOrUpdateCommand(runId, Cancelled(runId))
      case Failure(x) => println("Eventual response error occured: " + x.getMessage)
    }
  }

  private def processCommandWithMatcher(controlCommand: ControlCommand): Unit =
    controlCommand.commandName match {
      case `matcherTimeoutCmd` ⇒ Thread.sleep(1000)
      case `matcherFailedCmd` ⇒
        Source(1 to 10)
          .map(
            i ⇒
              currentStatePublisher.publish(
                CurrentState(controlCommand.source, StateName("testStateName"), Set(KeyType.IntKey.make("encoder").set(i * 1)))
            )
          )
          .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
          .runWith(Sink.ignore)
      case _ ⇒
        Source(1 to 10)
          .map(
            i ⇒
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
