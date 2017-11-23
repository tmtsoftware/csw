package csw.framework.internal.supervisor

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.scaladsl.{Actor, ActorContext, TimerScheduler}
import akka.typed.{ActorRef, Behavior, PostStop, Signal, SupervisorStrategy, Terminated}
import csw.exceptions.{FailureRestart, InitializationFailed}
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.scaladsl.ComponentBehaviorFactory
import csw.messages.CommandResponseManagerMessage.{Query, Subscribe, Unsubscribe}
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.FromSupervisorMessage.SupervisorLifecycleStateChanged
import csw.messages.RunningMessage.Lifecycle
import csw.messages.SupervisorCommonMessage._
import csw.messages.SupervisorIdleMessage._
import csw.messages.SupervisorInternalRunningMessage.{RegistrationFailed, RegistrationNotRequired, RegistrationSuccess}
import csw.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.messages.SupervisorRestartMessage.{UnRegistrationComplete, UnRegistrationFailed}
import csw.messages._
import csw.messages.framework.LocationServiceUsage.DoNotRegister
import csw.messages.framework.SupervisorLifecycleState.{Idle, RunningOffline}
import csw.messages.framework.{ComponentInfo, SupervisorLifecycleState}
import csw.messages.location.ComponentId
import csw.messages.location.Connection.AkkaConnection
import csw.messages.models.PubSub.Publish
import csw.messages.models.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.messages.models.{LifecycleStateChanged, LockingResponse, PubSub, ToComponentLifecycleMessage}
import csw.messages.params.states.CurrentState
import csw.services.ccs.internal.CommandResponseManagerFactory
import csw.services.location.models.AkkaRegistration
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success}

object SupervisorBehavior {
  val PubSubComponentActor        = "pub-sub-component"
  val PubSubLifecycleActor        = "pub-sub-lifecycle"
  val InitializeTimerKey          = "initialize-timer"
  val ComponentActorNameSuffix    = "component-actor"
  val CommandResponseManagerActor = "command-response-manager"
}

/**
 * The Behavior of a Supervisor of a component actor, represented as a mutable behavior.
 *
 * @param ctx                        The Actor Context under which the actor instance of this behavior is created
 * @param timerScheduler             Provides support for scheduled `self` messages in an actor
 * @param maybeContainerRef          The container ref of the container under which this supervisor is started if
 *                                   its not running in standalone mode
 * @param componentInfo              Component related information as described in the configuration file
 * @param componentBehaviorFactory   The factory for creating the component supervised by this Supervisor
 * @param pubSubBehaviorFactory      The factory for creating actor instance of [[csw.framework.internal.pubsub.PubSubBehavior]]
 *                                   for utilising pub-sub of any state of a component
 * @param registrationFactory        The factory for creating a typed [[csw.services.location.models.AkkaRegistration]] from
 *                                   [[csw.messages.location.Connection.AkkaConnection]]
 * @param locationService            The single instance of Location service created for a running application
 */
class SupervisorBehavior(
    ctx: ActorContext[SupervisorMessage],
    timerScheduler: TimerScheduler[SupervisorMessage],
    maybeContainerRef: Option[ActorRef[ContainerIdleMessage]],
    componentInfo: ComponentInfo,
    componentBehaviorFactory: ComponentBehaviorFactory[_],
    pubSubBehaviorFactory: PubSubBehaviorFactory,
    registrationFactory: RegistrationFactory,
    locationService: LocationService
) extends Actor.MutableBehavior[SupervisorMessage] {

  import SupervisorBehavior._
  import ctx.executionContext

  val loggerFactory                      = new LoggerFactory(componentInfo.name)
  val log: Logger                        = loggerFactory.getLogger(ctx)
  val componentName: String              = componentInfo.name
  val componentActorName: String         = s"$componentName-$ComponentActorNameSuffix"
  val initializeTimeout: FiniteDuration  = componentInfo.initializeTimeout
  val akkaConnection: AkkaConnection     = AkkaConnection(ComponentId(componentName, componentInfo.componentType))
  val akkaRegistration: AkkaRegistration = registrationFactory.akkaTyped(akkaConnection, ctx.self)
  val isStandalone: Boolean              = maybeContainerRef.isEmpty

  val pubSubComponent: ActorRef[PubSub[CurrentState]]                 = makePubSubComponent
  val pubSubLifecycle: ActorRef[PubSub[LifecycleStateChanged]]        = makePubSubLifecycle
  val commandResponseManager: ActorRef[CommandResponseManagerMessage] = makeCommandResponseManager

  var lifecycleState: SupervisorLifecycleState           = Idle
  var runningComponent: Option[ActorRef[RunningMessage]] = None
  var component: Option[ActorRef[Nothing]]               = None
  var lockManager: LockManager                           = new LockManager(None, loggerFactory)

  spawnAndWatchComponent()

  /**
   * Defines processing for a [[csw.messages.SupervisorMessage]] received by the actor instance.
   * @param msg      SupervisorMessage received
   * @return         The existing behavior
   */
  override def onMessage(msg: SupervisorMessage): Behavior[SupervisorMessage] = {
    log.debug(s"Supervisor in lifecycle state :[$lifecycleState] received message :[$msg]")
    (lifecycleState, msg) match {
      case (SupervisorLifecycleState.Lock, lockMessage: SupervisorLockMessage)          ⇒ onRunning(lockMessage)
      case (SupervisorLifecycleState.Lock, message)                                     ⇒ ignore(message)
      case (_, commonMessage: SupervisorCommonMessage)                                  ⇒ onCommon(commonMessage)
      case (SupervisorLifecycleState.Idle, idleMessage: SupervisorIdleMessage)          ⇒ onIdle(idleMessage)
      case (SupervisorLifecycleState.Restart, restartMessage: SupervisorRestartMessage) ⇒ onRestarting(restartMessage)
      case (SupervisorLifecycleState.Running, internalMessage: SupervisorInternalRunningMessage) ⇒
        onInternalRunning(internalMessage)
      case (SupervisorLifecycleState.Running, runningMessage: SupervisorRunningMessage) ⇒ onRunning(runningMessage)
      case (RunningOffline, runningMessage: SupervisorRunningMessage)                   ⇒ onRunning(runningMessage)
      case (_, message)                                                                 ⇒ ignore(message)
    }
    this
  }

  /**
   * Defines processing for a [[akka.typed.Signal]] received by the actor instance.
   * @return        The existing behavior
   */
  override def onSignal: PartialFunction[Signal, Behavior[SupervisorMessage]] = {
    case Terminated(componentRef) ⇒
      log.warn(s"Supervisor in lifecycle state :[$lifecycleState] received terminated signal from component :[$componentRef]")
      timerScheduler.cancel(InitializeTimerKey)

      lifecycleState match {
        case SupervisorLifecycleState.Restart  ⇒ spawn()
        case SupervisorLifecycleState.Shutdown ⇒ coordinatedShutdown()
        case SupervisorLifecycleState.Idle     ⇒ if (isStandalone) throw InitializationFailed
        case _                                 ⇒ updateLifecycleState(SupervisorLifecycleState.Idle) // Change to idle and expect Restart/Shutdown from outside
      }
      this
    case PostStop ⇒
      log.warn("Supervisor is shutting down. Un-registering supervisor from location service")
      locationService.unregister(akkaConnection)
      this
  }

  /**
   * Defines action for messages which can be received in any [[SupervisorLifecycleState]] state
   * @param commonMessage Message representing a message received in any lifecycle state
   */
  private def onCommon(commonMessage: SupervisorCommonMessage): Unit = commonMessage match {
    case LifecycleStateSubscription(subscriberMessage) ⇒ pubSubLifecycle ! subscriberMessage
    case ComponentStateSubscription(subscriberMessage) ⇒ pubSubComponent ! subscriberMessage
    case GetSupervisorLifecycleState(replyTo)          ⇒ replyTo ! lifecycleState
    case Restart                                       ⇒ onRestart()
    case Shutdown                                      ⇒ onShutdown()
  }

  /**
   * Defines action for messages which can be received in [[SupervisorLifecycleState.Idle]] state
   *
   * @param idleMessage  Message representing a message received in [[SupervisorLifecycleState.Idle]] state
   */
  private def onIdle(idleMessage: SupervisorIdleMessage): Unit = idleMessage match {
    case Running(componentRef) ⇒ onComponentRunning(componentRef)
    case InitializeTimeout     ⇒ onInitializeTimeout()
  }

  /**
   * Defines action for messages which can be received in [[SupervisorLifecycleState.Restart]] state
   *
   * @param restartMessage  Message representing a message received in [[SupervisorLifecycleState.Restart]] state
   */
  private def onRestarting(restartMessage: SupervisorRestartMessage): Unit = restartMessage match {
    case UnRegistrationComplete ⇒
      log.info("Supervisor unregistered itself from location service")
      respawnComponent()
    case UnRegistrationFailed(throwable) ⇒
      log.error(throwable.getMessage, ex = throwable)
      respawnComponent()
  }

  /**
   * Defines action for messages which can be received in [[SupervisorLifecycleState.Running]] state
   *
   * @param internalRunningMessage Message representing a message received in [[SupervisorLifecycleState.Running]] state
   */
  private def onInternalRunning(internalRunningMessage: SupervisorInternalRunningMessage): Unit = internalRunningMessage match {
    case RegistrationSuccess(componentRef)     ⇒ onRegistrationComplete(componentRef)
    case RegistrationNotRequired(componentRef) ⇒ onRegistrationComplete(componentRef)
    case RegistrationFailed(throwable)         ⇒ onRegistrationFailed(throwable)
  }

  /**
   * Defines action for messages which can be received in [[SupervisorLifecycleState.Running]] state
   *
   * @param runningMessage Message representing a message received in [[SupervisorLifecycleState.Running]] state
   */
  private def onRunning(runningMessage: SupervisorRunningMessage): Unit = runningMessage match {
    case Query(commandId, replyTo)       ⇒ commandResponseManager ! Query(commandId, replyTo)
    case Subscribe(commandId, replyTo)   ⇒ commandResponseManager ! Subscribe(commandId, replyTo)
    case Unsubscribe(commandId, replyTo) ⇒ commandResponseManager ! Unsubscribe(commandId, replyTo)
    case Lock(prefix, token, replyTo)    ⇒ lockComponent(prefix, token, replyTo)
    case Unlock(prefix, token, replyTo)  ⇒ unlockComponent(prefix, token, replyTo)
    case commandMessage: CommandMessage  ⇒ if (lockManager.allowCommand(commandMessage)) runningComponent.get ! commandMessage
    case runningMessage: RunningMessage  ⇒ handleRunningMessage(runningMessage)
    case msg @ Running(_)                ⇒ log.info(s"Ignoring [$msg] message received from TLA as Supervisor already in Running state")
  }

  private def lockComponent(prefix: String, token: String, replyTo: ActorRef[LockingResponse]): Unit = {
    lockManager = lockManager.lockComponent(prefix, token, replyTo)
    if (lockManager.lock.isDefined) updateLifecycleState(SupervisorLifecycleState.Lock)
  }

  private def unlockComponent(prefix: String, token: String, replyTo: ActorRef[LockingResponse]): Unit = {
    lockManager = lockManager.unlockComponent(prefix, token, replyTo)
    if (lockManager.lock.isEmpty) updateLifecycleState(SupervisorLifecycleState.Running)
  }

  private def handleRunningMessage(runningMessage: RunningMessage): Unit = {
    runningMessage match {
      case Lifecycle(message) ⇒ onLifeCycle(message)
      case _                  ⇒
    }
    runningComponent.get ! runningMessage
  }

  private def onRestart(): Unit = {
    updateLifecycleState(SupervisorLifecycleState.Restart)
    unRegisterFromLocationService()
  }

  private def onShutdown() = {
    updateLifecycleState(SupervisorLifecycleState.Shutdown)
    ctx.child(componentActorName) match {
      case Some(ref) ⇒ ctx.stop(ref) // stop component actor for a graceful shutdown before shutting down the actor system
      case None      ⇒ coordinatedShutdown()
    }
  }

  private def respawnComponent(): Unit = {
    log.info("Supervisor re-spawning component")
    ctx.child(componentActorName) match {
      case Some(_) ⇒ component.foreach(ctx.stop) // stop component actor for a graceful shutdown before restart
      case None    ⇒ spawn()
    }
  }

  private def spawn(): Unit = {
    updateLifecycleState(Idle)
    spawnAndWatchComponent()
  }

  private def onLifeCycle(message: ToComponentLifecycleMessage): Unit = message match {
    case GoOffline ⇒ if (lifecycleState == SupervisorLifecycleState.Running) updateLifecycleState(RunningOffline)
    case GoOnline  ⇒ if (lifecycleState == RunningOffline) updateLifecycleState(SupervisorLifecycleState.Running)
  }

  private def registerWithLocationService(componentRef: ActorRef[RunningMessage]): Unit = {
    if (componentInfo.locationServiceUsage == DoNotRegister) //Honour DoNotRegister received in componentInfo
      ctx.self ! RegistrationNotRequired(componentRef)
    else
      locationService.register(akkaRegistration).onComplete {
        case Success(_)         ⇒ ctx.self ! RegistrationSuccess(componentRef)
        case Failure(throwable) ⇒ ctx.self ! RegistrationFailed(throwable)
      }
  }

  private def unRegisterFromLocationService(): Unit = {
    log.debug(s"Un-registering supervisor from location service")
    locationService.unregister(akkaConnection).onComplete {
      case Success(_)         ⇒ ctx.self ! UnRegistrationComplete
      case Failure(throwable) ⇒ ctx.self ! UnRegistrationFailed(throwable)
    }
  }

  private def onRegistrationComplete(componentRef: ActorRef[RunningMessage]): Unit = {
    maybeContainerRef.foreach { container ⇒
      container ! SupervisorLifecycleStateChanged(ctx.self, lifecycleState)
      log.debug(s"Supervisor acknowledged container :[$container] for lifecycle state :[$lifecycleState]")
    }
    pubSubLifecycle ! Publish(LifecycleStateChanged(ctx.self, SupervisorLifecycleState.Running))
  }

  private def spawnAndWatchComponent(): Unit = {
    log.debug(s"Supervisor is spawning component TLA")
    component = Some(createTLA())
    log.info(s"Starting InitializeTimer for $initializeTimeout")
    timerScheduler.startSingleTimer(InitializeTimerKey, InitializeTimeout, initializeTimeout)
    component.foreach(ctx.watch) // watch created component to get notified when it’s terminated.
  }

  private def createTLA(): ActorRef[Nothing] = {
    val behavior = Actor
      .supervise[Nothing](
        componentBehaviorFactory.make(componentInfo, ctx.self, pubSubComponent, commandResponseManager, locationService)
      )
      .onFailure[FailureRestart](SupervisorStrategy.restartWithLimit(3, Duration.Zero).withLoggingEnabled(true))

    ctx.spawn[Nothing](behavior, componentActorName)
  }

  private def coordinatedShutdown(): Future[Done] = CoordinatedShutdown(ctx.system.toUntyped).run()

  private def makePubSubComponent(): ActorRef[PubSub[CurrentState]] =
    pubSubBehaviorFactory.make(ctx, PubSubComponentActor, loggerFactory)

  private def makePubSubLifecycle(): ActorRef[PubSub[LifecycleStateChanged]] =
    pubSubBehaviorFactory.make(ctx, PubSubLifecycleActor, loggerFactory)

  private def makeCommandResponseManager() = CommandResponseManagerFactory.make(ctx, CommandResponseManagerActor, loggerFactory)

  private def ignore(message: SupervisorMessage): Unit =
    log.error(s"Unexpected message :[$message] received by supervisor in lifecycle state :[$lifecycleState]")

  private def onInitializeTimeout(): Unit = {
    log.error("Component TLA initialization timed out")
    component.foreach(ctx.stop)
  }

  private def onComponentRunning(componentRef: ActorRef[RunningMessage]): Unit = {
    log.info("Received Running message from component within timeout, cancelling InitializeTimer")
    timerScheduler.cancel(InitializeTimerKey)

    updateLifecycleState(SupervisorLifecycleState.Running)
    runningComponent = Some(componentRef)
    registerWithLocationService(componentRef)
  }

  private def onRegistrationFailed(throwable: Throwable) = {
    updateLifecycleState(SupervisorLifecycleState.Idle)
    runningComponent = None
    log.error(throwable.getMessage, ex = throwable)
    throw throwable
  }

  private def updateLifecycleState(state: SupervisorLifecycleState): Unit = {
    log.debug(s"Supervisor is changing lifecycle state from [$lifecycleState] to [$state]")
    lifecycleState = state
  }
}
