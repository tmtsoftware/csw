package csw.common.components.command

import akka.actor
import akka.actor.Scheduler
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.util.Timeout
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.CommandResponseManagerMessage.{AddOrUpdateCommand, Query}
import csw.messages._
import csw.messages.ccs.CommandIssue.{OtherIssue, WrongPrefixIssue}
import csw.messages.ccs.commands.CommandResponse._
import csw.messages.ccs.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location._
import csw.messages.models.PubSub.{Publish, PublisherMessage}
import csw.messages.params.generics.{Key, KeyType, Parameter}
import csw.messages.params.models.RunId
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

class ComponentHandlerForCommand(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers[TopLevelActorDomainMessage](
      ctx,
      componentInfo,
      commandResponseManager,
      pubSubRef,
      locationService,
      loggerFactory
    ) {

  val log: Logger              = loggerFactory.getLogger(ctx)
  val cancel_move: Key[String] = KeyType.StringKey.make("cancel_move")

  import ComponentStateForCommand._
  implicit val actorSystem: actor.ActorSystem = ctx.system.toUntyped
  implicit val ec: ExecutionContext           = ctx.executionContext
  implicit val mat: ActorMaterializer         = ActorMaterializer()

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def onDomainMsg(msg: TopLevelActorDomainMessage): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = controlCommand.prefix match {
    case `acceptedCmdPrefix`    ⇒ Accepted(controlCommand.runId)
    case `withoutMatcherPrefix` ⇒ Accepted(controlCommand.runId)
    case `matcherPrefix`        ⇒ Accepted(controlCommand.runId)
    case `immediateCmdPrefix`   ⇒ Completed(controlCommand.runId)
    case `invalidCmdPrefix`     ⇒ Invalid(controlCommand.runId, OtherIssue(s"Unsupported prefix: ${controlCommand.prefix.prefix}"))
    case _                      ⇒ Invalid(controlCommand.runId, WrongPrefixIssue(s"Wrong prefix: ${controlCommand.prefix.prefix}"))
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = controlCommand.prefix match {
    case `acceptedCmdPrefix`    ⇒ processAcceptedSubmitCmd(controlCommand)
    case `withoutMatcherPrefix` ⇒ processCommandWithoutMatcher(controlCommand)
    case _                      ⇒ CommandNotAvailable(controlCommand.runId)
  }

  override def onOneway(controlCommand: ControlCommand): Unit = controlCommand.prefix match {
    case `acceptedCmdPrefix` ⇒ processAcceptedOnewayCmd(controlCommand)
    case `matcherPrefix`     ⇒ processCommandWithMatcher(controlCommand)
    case _                   ⇒ CommandNotAvailable(controlCommand.runId)
  }

  //If the command is cancel, only then process otherwise do nothing to mimic long running process
  private def processAcceptedSubmitCmd(controlCommand: ControlCommand): Unit =
    controlCommand.paramType.get(cancel_move).foreach { param ⇒
      processCancelCommand(controlCommand.runId, RunId(param.head))
    }

  //If the command is cancel, only then process otherwise do nothing to mimic long running process
  private def processAcceptedOnewayCmd(controlCommand: ControlCommand): Unit =
    controlCommand.paramType.get(cancel_move).foreach(param ⇒ processOriginalCommand(RunId(param.head)))

  private def processCommandWithoutMatcher(controlCommand: ControlCommand): Unit = {
    val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(20)
    val result                = Result(controlCommand.prefix, Set(param))
    commandResponseManager ! AddOrUpdateCommand(controlCommand.runId, CompletedWithResult(controlCommand.runId, result))
  }

  private def processCancelCommand(runId: RunId, cancelId: RunId): Unit = {
    processOriginalCommand(cancelId)
    commandResponseManager ! AddOrUpdateCommand(runId, Completed(runId))
  }

  private def processOriginalCommand(cancelId: RunId): Unit = {
    import akka.typed.scaladsl.AskPattern._
    implicit val timeout: Timeout     = 5.seconds
    implicit val scheduler: Scheduler = ctx.system.scheduler

    val eventualResponse: Future[CommandResponse] = commandResponseManager ? (Query(cancelId, _))
    eventualResponse.foreach { _ ⇒
      commandResponseManager ! AddOrUpdateCommand(cancelId, Cancelled(cancelId))
    }
  }

  private def processCommandWithMatcher(controlCommand: ControlCommand): Unit = {
    Source(1 to 10)
      .map(i ⇒ pubSubRef ! Publish(CurrentState(controlCommand.prefix, Set(KeyType.IntKey.make("encoder").set(i * 10)))))
      .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
      .runWith(Sink.ignore)
  }

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
