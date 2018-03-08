package csw.framework.internal.component

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior, PostStop, Signal}
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.RunningMessage.Lifecycle
import csw.messages.TopLevelActorCommonMessage.{TrackingEventReceived, UnderlyingHookFailed}
import csw.messages.TopLevelActorIdleMessage.Initialize
import csw.messages._
import csw.messages.commands.CommandResponse
import csw.messages.commands.CommandResponse.Accepted
import csw.messages.framework.LocationServiceUsage.RegisterAndTrackServices
import csw.messages.framework.ToComponentLifecycleMessages.{GoOffline, GoOnline}
import csw.messages.framework.{ComponentInfo, ToComponentLifecycleMessage}
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.async.Async.{async, await}
import scala.concurrent.Await
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.control.NonFatal

/**
 * The Behavior of a component actor, represented as a mutable behavior
 *
 * @param ctx                  The Actor Context under which the actor instance of this behavior is created
 * @param componentInfo        Component related information as described in the configuration file
 * @param supervisor           The actor reference of the supervisor actor which created this component
 * @param lifecycleHandlers    The implementation of handlers which defines the domain actions to be performed by this
 *                             component
 * @param locationService      The single instance of Location service created for a running application
 */
//TODO: add doc for significance for everything
class ComponentBehavior private[framework] (
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    supervisor: ActorRef[FromComponentLifecycleMessage],
    lifecycleHandlers: ComponentHandlers,
    commandResponseManager: CommandResponseManager,
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends Actor.MutableBehavior[TopLevelActorMessage] {

  import ctx.executionContext

  private val log: Logger = loggerFactory.getLogger(ctx)

  private val shutdownTimeout: FiniteDuration = 10.seconds

  private[framework] var lifecycleState: ComponentLifecycleState = ComponentLifecycleState.Idle

  ctx.self ! Initialize

  /**
   * Defines processing for a [[csw.messages.TopLevelActorMessage]] received by the actor instance.
   *
   * @param msg      ComponentMessage received from supervisor
   * @return         The existing behavior
   */
  def onMessage(msg: TopLevelActorMessage): Behavior[TopLevelActorMessage] = {
    log.debug(s"Component TLA in lifecycle state :[$lifecycleState] received message :[$msg]")
    (lifecycleState, msg) match {
      case (_, msg: TopLevelActorCommonMessage)                          ⇒ onCommon(msg)
      case (ComponentLifecycleState.Idle, msg: TopLevelActorIdleMessage) ⇒ onIdle(msg)
      case (ComponentLifecycleState.Running, msg: RunningMessage)        ⇒ onRun(msg)
      case _                                                             ⇒ log.error(s"Unexpected message :[$msg] received by component in lifecycle state :[$lifecycleState]")
    }
    this
  }

  /**
   * Defines processing for a [[akka.typed.Signal]] received by the actor instance.
   * @return        The existing behavior
   */
  override def onSignal: PartialFunction[Signal, Behavior[TopLevelActorMessage]] = {
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

  /**
   * Defines action for messages which can be received in any [[ComponentLifecycleState]] state
   * @param commonMessage Message representing a message received in any lifecycle state
   */
  private def onCommon(commonMessage: TopLevelActorCommonMessage): Unit = commonMessage match {
    case UnderlyingHookFailed(exception) ⇒
      log.error(exception.getMessage, ex = exception)
      throw exception
    case TrackingEventReceived(trackingEvent) ⇒
      lifecycleHandlers.onLocationTrackingEvent(trackingEvent)
  }

  /**
   * Defines action for messages which can be received in [[ComponentLifecycleState.Idle]] state
   * @param idleMessage  Message representing a message received in [[ComponentLifecycleState.Idle]] state
   */
  private def onIdle(idleMessage: TopLevelActorIdleMessage): Unit = idleMessage match {
    case Initialize ⇒
      async {
        log.info("Invoking lifecycle handler's initialize hook")
        await(lifecycleHandlers.initialize())
        log.debug(
          s"Component TLA is changing lifecycle state from [$lifecycleState] to [${ComponentLifecycleState.Initialized}]"
        )
        lifecycleState = ComponentLifecycleState.Initialized
        // track all connections in component info for location updates
        if (componentInfo.locationServiceUsage == RegisterAndTrackServices) {
          componentInfo.connections.foreach(
            connection ⇒ {
              locationService.subscribe(connection, trackingEvent ⇒ ctx.self ! TrackingEventReceived(trackingEvent))
            }
          )
        }
        lifecycleState = ComponentLifecycleState.Running
        lifecycleHandlers.isOnline = true
        supervisor ! Running(ctx.self)
      }.failed.foreach(throwable ⇒ ctx.self ! UnderlyingHookFailed(throwable))
  }

  /**
   * Defines action for messages which can be received in [[ComponentLifecycleState.Running]] state
   * @param runningMessage  Message representing a message received in [[ComponentLifecycleState.Running]] state
   */
  private def onRun(runningMessage: RunningMessage): Unit = runningMessage match {
    case Lifecycle(message) ⇒ onLifecycle(message)
    case x: CommandMessage  ⇒ onRunningCompCommandMessage(x)
    case msg                ⇒ log.error(s"Component TLA cannot handle message :[$msg]")
  }

  /**
   * Defines action for messages which alter the [[ComponentLifecycleState]] state
   * @param toComponentLifecycleMessage  Message representing a lifecycle message sent by the supervisor to the component
   */
  private def onLifecycle(toComponentLifecycleMessage: ToComponentLifecycleMessage): Unit =
    toComponentLifecycleMessage match {
      case GoOnline ⇒
        // process only if the component is offline currently
        if (!lifecycleHandlers.isOnline) {
          lifecycleHandlers.isOnline = true
          log.info("Invoking lifecycle handler's onGoOnline hook")
          lifecycleHandlers.onGoOnline()
          log.debug(s"Component TLA is Online")
        }
      case GoOffline ⇒
        // process only if the component is online currently
        if (lifecycleHandlers.isOnline) {
          lifecycleHandlers.isOnline = false
          log.info("Invoking lifecycle handler's onGoOffline hook")
          lifecycleHandlers.onGoOffline()
          log.debug(s"Component TLA is Offline")
        }
    }

  /**
   * Defines action for messages which represent a [[csw.messages.commands.Command]]
   *
   * @param commandMessage  Message encapsulating a [[csw.messages.commands.Command]]
   */
  private def onRunningCompCommandMessage(commandMessage: CommandMessage): Unit = {

    log.info(s"Invoking lifecycle handler's validateSubmit hook with msg :[$commandMessage]")
    val validationResponse = lifecycleHandlers.validateCommand(commandMessage.command)

    commandMessage match {
      case _: Submit ⇒
        commandResponseManager.commandResponseManagerActor ! AddOrUpdateCommand(commandMessage.command.runId, validationResponse)
      case _: Oneway ⇒ //Oneway command should not be added to CommandResponseManager
    }

    commandMessage.replyTo ! validationResponse
    forwardCommand(commandMessage, validationResponse)
  }

  private def forwardCommand(commandMessage: CommandMessage, validationResponse: CommandResponse): Unit =
    validationResponse match {
      case Accepted(_) ⇒
        commandMessage match {
          case _: Submit ⇒
            log.info(s"Invoking lifecycle handler's onSubmit hook with msg :[$commandMessage]")
            lifecycleHandlers.onSubmit(commandMessage.command)
          case _: Oneway ⇒
            log.info(s"Invoking lifecycle handler's onOneway hook with msg :[$commandMessage]")
            lifecycleHandlers.onOneway(commandMessage.command)
        }
      case _ ⇒ log.debug(s"Command not forwarded to TLA post validation. ValidationResponse was [$validationResponse]")
    }
}
