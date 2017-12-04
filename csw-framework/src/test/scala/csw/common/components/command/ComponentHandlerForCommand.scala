package csw.common.components.command

import akka.actor
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.messages._
import csw.messages.ccs.CommandIssue.{OtherIssue, WrongPrefixIssue}
import csw.messages.ccs.commands.CommandResponse._
import csw.messages.ccs.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location._
import csw.messages.models.PubSub.{Publish, PublisherMessage}
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

class ComponentHandlerForCommand(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers[ComponentDomainMessage](
      ctx,
      componentInfo,
      commandResponseManager,
      pubSubRef,
      locationService,
      loggerFactory
    ) {

  val log: Logger = loggerFactory.getLogger(ctx)

  import ComponentStateForCommand._
  implicit val actorSystem: actor.ActorSystem = ctx.system.toUntyped
  implicit val ec: ExecutionContext           = ctx.executionContext
  implicit val mat: ActorMaterializer         = ActorMaterializer()

  override def initialize(): Future[Unit] = Future.unit

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def onDomainMsg(msg: ComponentDomainMessage): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = controlCommand.prefix match {
    case `acceptedCmdPrefix`            ⇒ Accepted(controlCommand.runId)
    case `acceptWithNoMatcherCmdPrefix` ⇒ Accepted(controlCommand.runId)
    case `acceptWithMatcherCmdPrefix`   ⇒ Accepted(controlCommand.runId)
    case `immediateCmdPrefix`           ⇒ Completed(controlCommand.runId)
    case `invalidCmdPrefix`             ⇒ Invalid(controlCommand.runId, OtherIssue(s"Unsupported prefix: ${controlCommand.prefix.prefix}"))
    case _                              ⇒ Invalid(controlCommand.runId, WrongPrefixIssue(s"Wrong prefix: ${controlCommand.prefix.prefix}"))
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = controlCommand.prefix match {
    case `acceptWithNoMatcherCmdPrefix` ⇒ processCommandWithoutMatcher(controlCommand)
    case _                              ⇒ CommandNotAvailable(controlCommand.runId)
  }

  override def onOneway(controlCommand: ControlCommand): Unit = controlCommand.prefix match {
    case `acceptWithMatcherCmdPrefix` ⇒ processCommandWithMatcher(controlCommand)
    case _                            ⇒ CommandNotAvailable(controlCommand.runId)
  }

  private def processCommandWithoutMatcher(controlCommand: ControlCommand): Unit = {
    val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(20)
    val result                = Result(controlCommand.runId, controlCommand.prefix, controlCommand.maybeObsId, Set(param))
    commandResponseManager ! AddOrUpdateCommand(controlCommand.runId, CompletedWithResult(controlCommand.runId, result))
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
