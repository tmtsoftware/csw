package csw.framework.internal.component

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior, PostStop, Signal}
import csw.framework.models.ComponentInfo
import csw.framework.models.LocationServiceUsage.RegisterAndTrackServices
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.messages.CommandMessage.{Oneway, Submit}
import csw.messages.messages.CommonMessage.{TrackingEventReceived, UnderlyingHookFailed}
import csw.messages.messages.FromComponentLifecycleMessage.{Initialized, Running}
import csw.messages.messages.IdleMessage.Initialize
import csw.messages.messages.InitialMessage.Run
import csw.messages.messages.RunningMessage.{DomainMessage, Lifecycle}
import csw.messages.messages.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.messages.messages._
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.ComponentLogger

import scala.async.Async.{async, await}
import scala.concurrent.Await
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

class ComponentBehavior[Msg <: DomainMessage: ClassTag](
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    supervisor: ActorRef[FromComponentLifecycleMessage],
    lifecycleHandlers: ComponentHandlers[Msg],
    locationService: LocationService
) extends ComponentLogger.TypedActor[ComponentMessage](ctx, Some(componentInfo.name)) {

  import ctx.executionContext

  val shutdownTimeout: FiniteDuration = 10.seconds

  var lifecycleState: ComponentLifecycleState = ComponentLifecycleState.Idle

  ctx.self ! Initialize

  def onMessage(msg: ComponentMessage): Behavior[ComponentMessage] = {
    log.debug(s"Component TLA in lifecycle state :[$lifecycleState] received message :[$msg]")
    (lifecycleState, msg) match {
      case (_, msg: CommonMessage)                                    ⇒ onCommon(msg)
      case (ComponentLifecycleState.Idle, msg: IdleMessage)           ⇒ onIdle(msg)
      case (ComponentLifecycleState.Initialized, msg: InitialMessage) ⇒ onInitial(msg)
      case (ComponentLifecycleState.Running, msg: RunningMessage)     ⇒ onRun(msg)
      case _                                                          ⇒ log.error(s"Unexpected message :[$msg] received by component in lifecycle state :[$lifecycleState]")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[ComponentMessage]] = {
    case PostStop ⇒
      log.warn("Component TLA is shutting down")
      try {
        log.info("Invoking lifecycle handler's onShutdown hook")
        Await.result(lifecycleHandlers.onShutdown(), shutdownTimeout)
      } catch {
        case NonFatal(throwable) ⇒ log.error(throwable.getMessage, ex = throwable)
      }
      this
  }

  private def onCommon(commonMessage: CommonMessage): Unit = commonMessage match {
    case UnderlyingHookFailed(exception) ⇒
      log.error(exception.getMessage, ex = exception)
      throw exception
    case TrackingEventReceived(trackingEvent) ⇒
      lifecycleHandlers.onLocationTrackingEvent(trackingEvent)
  }

  private def onIdle(idleMessage: IdleMessage): Unit = idleMessage match {
    case Initialize ⇒
      async {
        log.info("Invoking lifecycle handler's initialize hook")
        await(lifecycleHandlers.initialize())
        log.debug(
          s"Component TLA is changing lifecycle state from [$lifecycleState] to [${ComponentLifecycleState.Initialized}]"
        )
        lifecycleState = ComponentLifecycleState.Initialized
        if (componentInfo.locationServiceUsage == RegisterAndTrackServices) {
          componentInfo.connections.foreach(
            connection ⇒ {
              locationService.subscribe(connection, trackingEvent ⇒ ctx.self ! TrackingEventReceived(trackingEvent))
            }
          )
        }
        supervisor ! Initialized(ctx.self)
      }.failed.foreach(throwable ⇒ ctx.self ! UnderlyingHookFailed(throwable))
  }

  private def onInitial(initialMessage: InitialMessage): Unit = initialMessage match {
    case Run ⇒
      async {
        log.info("Invoking lifecycle handler's onRun hook")
        await(lifecycleHandlers.onRun())
        log.debug(
          s"Component TLA is changing lifecycle state from [$lifecycleState] to [${ComponentLifecycleState.Running}]"
        )
        lifecycleState = ComponentLifecycleState.Running
        lifecycleHandlers.isOnline = true
        supervisor ! Running(ctx.self)
      }.failed.foreach(throwable ⇒ ctx.self ! UnderlyingHookFailed(throwable))
  }

  private def onRun(runningMessage: RunningMessage): Unit = runningMessage match {
    case Lifecycle(message) ⇒ onLifecycle(message)
    case x: Msg ⇒
      log.info(s"Invoking lifecycle handler's onDomainMsg hook with msg :[$x]")
      lifecycleHandlers.onDomainMsg(x)
    case x: CommandMessage            ⇒ onRunningCompCommandMessage(x)
    case x: CommandValidationResponse ⇒ lifecycleHandlers.onCommandValidationNotification(x)
    case x: CommandExecutionResponse  ⇒ lifecycleHandlers.onCommandExecutionNotification(x)
    case msg                          ⇒ log.error(s"Component TLA cannot handle message :[$msg]")
  }

  private def onLifecycle(toComponentLifecycleMessage: ToComponentLifecycleMessage): Unit =
    toComponentLifecycleMessage match {
      case GoOnline ⇒
        if (!lifecycleHandlers.isOnline) {
          lifecycleHandlers.isOnline = true
          log.info("Invoking lifecycle handler's onGoOnline hook")
          lifecycleHandlers.onGoOnline()
          log.debug(s"Component TLA is Online")
        }
      case GoOffline ⇒
        if (lifecycleHandlers.isOnline) {
          lifecycleHandlers.isOnline = false
          log.info("Invoking lifecycle handler's onGoOffline hook")
          lifecycleHandlers.onGoOffline()
          log.debug(s"Component TLA is Offline")
        }
    }

  def onRunningCompCommandMessage(commandMessage: CommandMessage): Unit = {
    val newMessage: CommandMessage = commandMessage match {
      case x: Oneway ⇒ x.copy(replyTo = ctx.spawnAnonymous(Actor.ignore))
      case x: Submit ⇒ x
    }
    log.info(s"Invoking lifecycle handler's onControlCommand hook with msg :[$newMessage]")
    val validation              = lifecycleHandlers.onControlCommand(newMessage)
    val validationCommandResult = CommandValidationResponse.validationAsCommandStatus(validation)
    commandMessage.replyTo ! validationCommandResult
  }
}
