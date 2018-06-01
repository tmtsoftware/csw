package csw.common.components.command

import akka.actor
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.util.Timeout
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.commands.CommandIssue.{OtherIssue, WrongPrefixIssue}
import csw.messages.commands.CommandResponse._
import csw.messages.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location._
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.Id
import csw.messages.params.states.{CurrentState, StateName}
import csw.messages.scaladsl.TopLevelActorMessage
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

class ComponentHandlerForCommand(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory) {

  private val cancelCmdId = KeyType.StringKey.make("cancelCmdId")

  import ComponentStateForCommand._
  implicit val actorSystem: actor.ActorSystem = ctx.system.toUntyped
  implicit val ec: ExecutionContext           = ctx.executionContext
  implicit val mat: ActorMaterializer         = ActorMaterializer()

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = controlCommand.commandName match {
    case `acceptedCmd`       ⇒ Accepted(controlCommand.runId)
    case `withoutMatcherCmd` ⇒ Accepted(controlCommand.runId)
    case `matcherCmd`        ⇒ Accepted(controlCommand.runId)
    case `matcherFailedCmd`  ⇒ Accepted(controlCommand.runId)
    case `matcherTimeoutCmd` ⇒ Accepted(controlCommand.runId)
    case `cancelCmd`         ⇒ Accepted(controlCommand.runId)
    case `immediateCmd`      ⇒ Completed(controlCommand.runId)
    case `immediateResCmd` ⇒
      CompletedWithResult(controlCommand.runId, Result(controlCommand.source, Set(KeyType.IntKey.make("encoder").set(20))))
    case `invalidCmd` ⇒ Invalid(controlCommand.runId, OtherIssue(s"Unsupported prefix: ${controlCommand.commandName}"))
    case _            ⇒ Invalid(controlCommand.runId, WrongPrefixIssue(s"Wrong prefix: ${controlCommand.commandName}"))
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = controlCommand.commandName match {
    case `cancelCmd`         ⇒ processAcceptedSubmitCmd(controlCommand)
    case `withoutMatcherCmd` ⇒ processCommandWithoutMatcher(controlCommand)
    case `acceptedCmd`       ⇒ //mimic long running process by not updating CSRM
    case _                   ⇒ CommandNotAvailable(controlCommand.runId)
  }

  override def onOneway(controlCommand: ControlCommand): Unit = controlCommand.commandName match {
    case `cancelCmd`        ⇒ processAcceptedOnewayCmd(controlCommand)
    case `matcherCmd`       ⇒ processCommandWithMatcher(controlCommand)
    case `matcherFailedCmd` ⇒ processCommandWithMatcher(controlCommand)
    case `acceptedCmd`      ⇒ //mimic long running process by publishing any state
    case _                  ⇒ CommandNotAvailable(controlCommand.runId)
  }

  private def processAcceptedSubmitCmd(controlCommand: ControlCommand): Unit = {
    controlCommand.paramType.get(cancelCmdId).foreach { param ⇒
      processCancelCommand(controlCommand.runId, Id(param.head))
    }
  }

  private def processAcceptedOnewayCmd(controlCommand: ControlCommand): Unit =
    controlCommand.paramType.get(cancelCmdId).foreach(param ⇒ processOriginalCommand(Id(param.head)))

  private def processCommandWithoutMatcher(controlCommand: ControlCommand): Unit = {
    val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(20)
    val result                = Result(controlCommand.source, Set(param))

    // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
    commandResponseManager.addOrUpdateCommand(controlCommand.runId, CompletedWithResult(controlCommand.runId, result))
  }

  private def processCancelCommand(runId: Id, cancelId: Id): Unit = {
    processOriginalCommand(cancelId)
    commandResponseManager.addOrUpdateCommand(runId, Completed(runId))
  }

  private def processOriginalCommand(runId: Id): Unit = {
    implicit val timeout: Timeout = 5.seconds

    // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
    val eventualResponse: Future[CommandResponse] = commandResponseManager.query(runId)
    eventualResponse.onComplete { x ⇒
      commandResponseManager.addOrUpdateCommand(runId, Cancelled(runId))
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
