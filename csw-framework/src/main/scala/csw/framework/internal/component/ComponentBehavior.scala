package csw.framework.internal.component

import akka.actor.typed.scaladsl.{ActorContext, MutableBehavior}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import csw.command.messages.CommandMessage.{Oneway, Submit}
import csw.command.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.command.messages.FromComponentLifecycleMessage.Running
import csw.command.messages.RunningMessage.Lifecycle
import csw.command.messages.TopLevelActorCommonMessage.{TrackingEventReceived, UnderlyingHookFailed}
import csw.command.messages.TopLevelActorIdleMessage.Initialize
import csw.command.messages._
import csw.command.models.framework.LocationServiceUsage.RegisterAndTrackServices
import csw.command.models.framework.ToComponentLifecycleMessage
import csw.command.models.framework.ToComponentLifecycleMessages.{GoOffline, GoOnline}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.framework.LocationServiceUsage.RegisterAndTrackServices
import csw.messages.framework.ToComponentLifecycleMessages.{GoOffline, GoOnline}
import csw.messages.framework.{ComponentInfo, ToComponentLifecycleMessage}
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.RunningMessage.Lifecycle
import csw.messages.TopLevelActorCommonMessage.{TrackingEventReceived, UnderlyingHookFailed}
import csw.messages.TopLevelActorIdleMessage.Initialize
import csw.messages._
import csw.messages.commands.CommandResponse._
import csw.services.command.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.{Logger, LoggerFactory}
import csw.messages.commands.CommandResponse
import csw.messages.commands.CommandResponse.Accepted
import csw.messages.framework.LocationServiceUsage.RegisterAndTrackServices
import csw.messages.framework.ToComponentLifecycleMessage
import csw.messages.framework.ToComponentLifecycleMessages.{GoOffline, GoOnline}
import csw.services.logging.scaladsl.Logger
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.Accepted
import csw.logging.scaladsl.Logger

import scala.async.Async.{async, await}
import scala.concurrent.Await
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.control.NonFatal

/**
 * The Behavior of a component actor, represented as a mutable behavior
 *
 * @param ctx the [[akka.actor.typed.scaladsl.ActorContext]] under which the actor instance of this behavior is created
 * @param supervisor the actor reference of the supervisor actor which created this component
 * @param lifecycleHandlers the implementation of handlers which defines the domain actions to be performed by this component
 */
private[framework] final class ComponentBehavior(
    ctx: ActorContext[TopLevelActorMessage],
    supervisor: ActorRef[FromComponentLifecycleMessage],
    lifecycleHandlers: ComponentHandlers,
    cswCtx: CswContext
) extends MutableBehavior[TopLevelActorMessage] {

  import cswCtx._
  import ctx.executionContext

  private val log: Logger = loggerFactory.getLogger(ctx)

  private val shutdownTimeout: FiniteDuration = 10.seconds

  private[framework] var lifecycleState: ComponentLifecycleState = ComponentLifecycleState.Idle

  ctx.self ! Initialize

  /**
   * Defines processing for a [[TopLevelActorMessage]] received by the actor instance.
   *
   * @param msg componentMessage received from supervisor
   * @return the existing behavior
   */
  def onMessage(msg: TopLevelActorMessage): Behavior[TopLevelActorMessage] = {
    log.debug(s"Component TLA in lifecycle state :[$lifecycleState] received message :[$msg]")
    (lifecycleState, msg) match {
      case (_, msg: TopLevelActorCommonMessage)                          ⇒ onCommon(msg)
      case (ComponentLifecycleState.Idle, msg: TopLevelActorIdleMessage) ⇒ onIdle(msg)
      case (ComponentLifecycleState.Running, msg: RunningMessage)        ⇒ onRun(msg)
      case _ ⇒
        log.error(s"Unexpected message :[$msg] received by component in lifecycle state :[$lifecycleState]")
    }
    this
  }

  /**
   * Defines processing for a [[akka.actor.typed.Signal]] received by the actor instance
   *
   * @return the existing behavior
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
   *
   * @param commonMessage message representing a message received in any lifecycle state
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
   *
   * @param idleMessage message representing a message received in [[ComponentLifecycleState.Idle]] state
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
   *
   * @param runningMessage message representing a message received in [[ComponentLifecycleState.Running]] state
   */
  private def onRun(runningMessage: RunningMessage): Unit = runningMessage match {
    case Lifecycle(message) ⇒ onLifecycle(message)
    case x: CommandMessage  ⇒ onRunningCompCommandMessage(x)
    case msg                ⇒ log.error(s"Component TLA cannot handle message :[$msg]")
  }

  /**
   * Defines action for messages which alter the [[ComponentLifecycleState]] state
   *
   * @param toComponentLifecycleMessage message representing a lifecycle message sent by the supervisor to the component
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
   * Defines action for messages which represent a [[csw.params.commands.Command]]
   *
   * @param commandMessage message encapsulating a [[csw.params.commands.Command]]
   */
  private def onRunningCompCommandMessage(commandMessage: CommandMessage): Unit = {

    log.info(s"Invoking lifecycle handler's validateSubmit hook with msg :[$commandMessage]")

    commandMessage match {
      case ow: Oneway ⇒ //Oneway command should not be added to CommandResponseManager
        val validationResponse = lifecycleHandlers.validateCommand(commandMessage.command)
        if (validationResponse == Accepted(commandMessage.command.runId)) {
          log.info(s"Invoking lifecycle handler's onOneway hook with msg :[$commandMessage]")
          lifecycleHandlers.onOneway(commandMessage.command)
        }
        ow.replyTo ! validationResponse.asInstanceOf[OnewayResponse]
      case su: Submit ⇒
        val validationResponse = lifecycleHandlers.validateCommand(commandMessage.command)
        validationResponse match {
          case _: Accepted =>
            // Here the submit is marked as started, to indicate that the command has transitioned from validation
            // and has started processing. This is needed also for the case where someone in onSubmit subscribes
            // to their own runId, which is done in at least one of the tests
            // Prefer not to add a new unique response that would only be relevant to the internals
            commandResponseManager.commandResponseManagerActor ! AddOrUpdateCommand(commandMessage.command.runId,
                                                                                    Started(commandMessage.command.runId))
            val submitResponse: SubmitResponse = lifecycleHandlers.onSubmit(commandMessage.command)

            // The response is used to update the CRM, it may still be Started it if is a long running command
            commandResponseManager.commandResponseManagerActor ! AddOrUpdateCommand(commandMessage.command.runId, submitResponse)
            su.replyTo ! submitResponse
          case inv: Invalid =>
            su.replyTo ! inv
        }
    }
  }

}
