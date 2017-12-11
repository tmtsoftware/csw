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
import csw.messages.params.generics.{KeyType, Parameter}
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

  val log: Logger = loggerFactory.getLogger(ctx)
  val cancelCmdId = KeyType.StringKey.make("cancelCmdId")

  import ComponentStateForCommand._
  implicit val actorSystem: actor.ActorSystem = ctx.system.toUntyped
  implicit val ec: ExecutionContext           = ctx.executionContext
  implicit val mat: ActorMaterializer         = ActorMaterializer()

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def onDomainMsg(msg: TopLevelActorDomainMessage): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = controlCommand.target match {
    case `acceptedCmdPrefix`    ⇒ Accepted(controlCommand.runId)
    case `withoutMatcherPrefix` ⇒ Accepted(controlCommand.runId)
    case `matcherPrefix`        ⇒ Accepted(controlCommand.runId)
    case `cancelCmdPrefix`      ⇒ Accepted(controlCommand.runId)
    case `immediateCmdPrefix`   ⇒ Completed(controlCommand.runId)
    case `invalidCmdPrefix` ⇒
      Invalid(controlCommand.runId, OtherIssue(s"Unsupported prefix: ${controlCommand.target.prefix}"))
    case _ ⇒ Invalid(controlCommand.runId, WrongPrefixIssue(s"Wrong prefix: ${controlCommand.target.prefix}"))
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = controlCommand.target match {
    case `cancelCmdPrefix`      ⇒ processAcceptedSubmitCmd(controlCommand)
    case `withoutMatcherPrefix` ⇒ processCommandWithoutMatcher(controlCommand)
    case `acceptedCmdPrefix`    ⇒ //mimic long running process by not updating CSRM
    case _                      ⇒ CommandNotAvailable(controlCommand.runId)
  }

  override def onOneway(controlCommand: ControlCommand): Unit = controlCommand.target match {
    case `cancelCmdPrefix`   ⇒ processAcceptedOnewayCmd(controlCommand)
    case `matcherPrefix`     ⇒ processCommandWithMatcher(controlCommand)
    case `acceptedCmdPrefix` ⇒ //mimic long running process by publishing any state
    case _                   ⇒ CommandNotAvailable(controlCommand.runId)
  }

  private def processAcceptedSubmitCmd(controlCommand: ControlCommand): Unit =
    controlCommand.paramType.get(cancelCmdId).foreach { param ⇒
      processCancelCommand(controlCommand.runId, RunId(param.head))
    }

  private def processAcceptedOnewayCmd(controlCommand: ControlCommand): Unit =
    controlCommand.paramType.get(cancelCmdId).foreach(param ⇒ processOriginalCommand(RunId(param.head)))

  private def processCommandWithoutMatcher(controlCommand: ControlCommand): Unit = {
    val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(20)
    val result                = Result(controlCommand.target, Set(param))
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
      .map(i ⇒ pubSubRef ! Publish(CurrentState(controlCommand.target, Set(KeyType.IntKey.make("encoder").set(i * 10)))))
      .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
      .runWith(Sink.ignore)
  }

  override def onShutdown(): Future[Unit] = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
