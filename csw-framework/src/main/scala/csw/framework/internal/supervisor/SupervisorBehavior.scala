package csw.framework.internal.supervisor

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.scaladsl.{Actor, ActorContext, TimerScheduler}
import akka.typed.{ActorRef, Behavior, PostStop, Signal, SupervisorStrategy, Terminated}
import csw.exceptions.{FailureRestart, InitializationFailed}
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.models.LocationServiceUsage.DoNotRegister
import csw.framework.models._
import csw.framework.scaladsl.ComponentBehaviorFactory
import csw.messages.FromComponentLifecycleMessage.{Initialized, Running}
import csw.messages.FromSupervisorMessage.SupervisorLifecycleStateChanged
import csw.messages.InitialMessage.Run
import csw.messages.PubSub.Publish
import csw.messages.RunningMessage.Lifecycle
import csw.messages.SupervisorCommonMessage.{
  ComponentStateSubscription,
  GetSupervisorLifecycleState,
  LifecycleStateSubscription
}
import csw.messages.SupervisorIdleMessage._
import csw.messages.SupervisorRestartMessage.{UnRegistrationComplete, UnRegistrationFailed}
import csw.messages.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.messages._
import csw.messages.models.location.ComponentId
import csw.messages.models.location.Connection.AkkaConnection
import csw.messages.states.SupervisorLifecycleState.Idle
import csw.messages.states.{CurrentState, SupervisorLifecycleState}
import csw.services.location.models.AkkaRegistration
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success}

object SupervisorBehavior {
  val PubSubComponentActor     = "pub-sub-component"
  val PubSubLifecycleActor     = "pub-sub-lifecycle"
  val InitializeTimerKey       = "initialize-timer"
  val RunTimerKey              = "run-timer"
  val ComponentActorNameSuffix = "component-actor"
}

class SupervisorBehavior(
    ctx: ActorContext[SupervisorMessage],
    timerScheduler: TimerScheduler[SupervisorMessage],
    maybeContainerRef: Option[ActorRef[ContainerIdleMessage]],
    componentInfo: ComponentInfo,
    componentBehaviorFactory: ComponentBehaviorFactory[_],
    pubSubBehaviorFactory: PubSubBehaviorFactory,
    registrationFactory: RegistrationFactory,
    locationService: LocationService
) extends ComponentLogger.TypedActor[SupervisorMessage](ctx, Some(componentInfo.name)) {

  import SupervisorBehavior._
  import ctx.executionContext

  val componentName: String              = componentInfo.name
  val componentActorName                 = s"$componentName-$ComponentActorNameSuffix"
  val initializeTimeout: FiniteDuration  = componentInfo.initializeTimeout
  val runTimeout: FiniteDuration         = componentInfo.runTimeout
  val akkaConnection                     = AkkaConnection(ComponentId(componentName, componentInfo.componentType))
  val akkaRegistration: AkkaRegistration = registrationFactory.akkaTyped(akkaConnection, ctx.self)
  val isStandalone: Boolean              = maybeContainerRef.isEmpty

  val pubSubComponent: ActorRef[PubSub[CurrentState]] =
    pubSubBehaviorFactory.make(ctx, PubSubComponentActor, componentName)
  val pubSubLifecycle: ActorRef[PubSub[LifecycleStateChanged]] =
    pubSubBehaviorFactory.make(ctx, PubSubLifecycleActor, componentName)

  var lifecycleState: SupervisorLifecycleState           = Idle
  var runningComponent: Option[ActorRef[RunningMessage]] = None
  var component: Option[ActorRef[Nothing]]               = None

  spawnAndWatchComponent()

  override def onMessage(msg: SupervisorMessage): Behavior[SupervisorMessage] = {
    log.debug(s"Supervisor in lifecycle state :[$lifecycleState] received message :[$msg]")
    (lifecycleState, msg) match {
      case (_, commonMessage: SupervisorCommonMessage)                                  ⇒ onCommon(commonMessage)
      case (SupervisorLifecycleState.Idle, idleMessage: SupervisorIdleMessage)          ⇒ onIdle(idleMessage)
      case (SupervisorLifecycleState.Restart, restartMessage: SupervisorRestartMessage) ⇒ onRestarting(restartMessage)
      case (SupervisorLifecycleState.Running | SupervisorLifecycleState.RunningOffline,
            runningMessage: SupervisorRunningMessage) ⇒
        onRunning(runningMessage)
      case (_, message) =>
        log.error(s"Unexpected message :[$message] received by supervisor in lifecycle state :[$lifecycleState]")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[SupervisorMessage]] = {
    case Terminated(componentRef) ⇒
      log.warn(
        s"Supervisor in lifecycle state :[$lifecycleState] received terminated signal from component :[$componentRef]"
      )
      timerScheduler.cancel(InitializeTimerKey)
      timerScheduler.cancel(RunTimerKey)
      lifecycleState match {
        case SupervisorLifecycleState.Restart  ⇒ spawn()
        case SupervisorLifecycleState.Shutdown ⇒ coordinatedShutdown()
        case SupervisorLifecycleState.Idle ⇒
          if (isStandalone) throw InitializationFailed
        case _ ⇒
      }
      this
    case PostStop ⇒
      log.warn("Supervisor is shutting down. Un-registering supervisor from location service")
      locationService.unregister(akkaConnection)
      this
  }

  def onCommon(msg: SupervisorCommonMessage): Unit = msg match {
    case LifecycleStateSubscription(subscriberMessage) ⇒ pubSubLifecycle ! subscriberMessage
    case ComponentStateSubscription(subscriberMessage) ⇒ pubSubComponent ! subscriberMessage
    case GetSupervisorLifecycleState(replyTo)          ⇒ replyTo ! lifecycleState
    case Restart                                       ⇒ onRestart()
    case Shutdown ⇒
      log.debug(
        s"Supervisor is changing lifecycle state from [$lifecycleState] to [${SupervisorLifecycleState.Shutdown}]"
      )
      lifecycleState = SupervisorLifecycleState.Shutdown
      ctx.child(componentActorName) match {
        case Some(componentRef) ⇒ ctx.stop(componentRef)
        case None               ⇒ coordinatedShutdown()
      }
  }

  def onIdle(msg: SupervisorIdleMessage): Unit = msg match {
    case Initialized(componentRef) ⇒
      log.info("Received Initialized message from component within timeout, cancelling InitializeTimer")
      timerScheduler.cancel(InitializeTimerKey)
      registerWithLocationService(componentRef)
    case RegistrationSuccess(componentRef) ⇒
      onRegistrationComplete(componentRef)
    case RegistrationNotRequired(componentRef) ⇒
      onRegistrationComplete(componentRef)
    case RegistrationFailed(throwable) ⇒
      log.error(throwable.getMessage, ex = throwable)
      throw throwable
    case Running(componentRef) ⇒
      log.info("Received Running message from component within timeout, cancelling RunTimer")
      timerScheduler.cancel(RunTimerKey)
      log.debug(
        s"Supervisor is changing lifecycle state from [$lifecycleState] to [${SupervisorLifecycleState.Running}]"
      )
      lifecycleState = SupervisorLifecycleState.Running
      runningComponent = Some(componentRef)
      maybeContainerRef foreach { container ⇒
        container ! SupervisorLifecycleStateChanged(ctx.self, lifecycleState)
        log.debug(s"Supervisor acknowledged container :[$container] for lifecycle state :[$lifecycleState]")
      }
      pubSubLifecycle ! Publish(LifecycleStateChanged(ctx.self, SupervisorLifecycleState.Running))
    case InitializeTimeout ⇒
      log.error("Component TLA initialization timed out")
      component.foreach(ctx.stop)
    case RunTimeout ⇒
      log.error("Component TLA onRun timed out")
      component.foreach(ctx.stop)
  }

  private def onRegistrationComplete(componentRef: ActorRef[InitialMessage]): Unit = {
    log.info(s"Starting RunTimer for $runTimeout")
    timerScheduler.startSingleTimer(RunTimerKey, RunTimeout, runTimeout)
    componentRef ! Run
  }

  private def onRunning(supervisorRunningMessage: SupervisorRunningMessage): Unit = {
    supervisorRunningMessage match {
      case runningMessage: RunningMessage ⇒
        runningMessage match {
          case Lifecycle(message) ⇒ onLifeCycle(message)
          case _                  ⇒
        }
        runningComponent.get ! runningMessage
    }
  }

  private def onRestart(): Unit = {
    log.debug(s"Supervisor is changing lifecycle state from [$lifecycleState] to [${SupervisorLifecycleState.Restart}]")
    lifecycleState = SupervisorLifecycleState.Restart
    unRegisterFromLocationService()
  }

  private def onRestarting(msg: SupervisorRestartMessage): Unit = msg match {
    case UnRegistrationComplete ⇒
      log.info("Supervisor unregistered itself from location service")
      respawnComponent()
    case UnRegistrationFailed(throwable) ⇒
      log.error(throwable.getMessage, ex = throwable)
      respawnComponent()
  }

  private def respawnComponent(): Unit = {
    log.info("Supervisor re-spawning component")
    ctx.child(componentActorName) match {
      case Some(_) ⇒ component.foreach(ctx.stop)
      case None    ⇒ spawn()
    }
  }

  private def spawn(): Unit = {
    log.debug(
      s"Supervisor is changing lifecycle state from [$lifecycleState] to [${SupervisorLifecycleState.Idle}]"
    )
    lifecycleState = SupervisorLifecycleState.Idle
    spawnAndWatchComponent()
  }

  private def onLifeCycle(message: ToComponentLifecycleMessage): Unit = {
    message match {
      case GoOffline ⇒
        if (lifecycleState == SupervisorLifecycleState.Running) {
          log.debug(
            s"Supervisor is changing lifecycle state from [$lifecycleState] to [${SupervisorLifecycleState.RunningOffline}]"
          )
          lifecycleState = SupervisorLifecycleState.RunningOffline
        }
      case GoOnline ⇒
        if (lifecycleState == SupervisorLifecycleState.RunningOffline) {
          log.debug(
            s"Supervisor is changing lifecycle state from [$lifecycleState] to [${SupervisorLifecycleState.Running}]"
          )
          lifecycleState = SupervisorLifecycleState.Running
        }
    }
  }

  private def registerWithLocationService(componentRef: ActorRef[InitialMessage]): Unit = {
    //Honour DoNotRegister received in componentInfo
    if (componentInfo.locationServiceUsage == DoNotRegister)
      ctx.self ! RegistrationNotRequired(componentRef)
    else {
      locationService.register(akkaRegistration).onComplete {
        case Success(registrationResult) ⇒ ctx.self ! RegistrationSuccess(componentRef)
        case Failure(throwable)          ⇒ ctx.self ! RegistrationFailed(throwable)
      }
    }
  }

  private def unRegisterFromLocationService(): Unit = {
    log.debug(s"Un-registering supervisor from location service")
    locationService.unregister(akkaConnection).onComplete {
      case Success(_)         ⇒ ctx.self ! UnRegistrationComplete
      case Failure(throwable) ⇒ ctx.self ! UnRegistrationFailed(throwable)
    }
  }

  private def spawnAndWatchComponent(): Unit = {
    log.debug(s"Supervisor is spawning component TLA")
    component = Some(
      ctx.spawn[Nothing](
        Actor
          .supervise[Nothing](componentBehaviorFactory.make(componentInfo, ctx.self, pubSubComponent, locationService))
          .onFailure[FailureRestart](SupervisorStrategy.restartWithLimit(3, Duration.Zero).withLoggingEnabled(true)),
        componentActorName
      )
    )
    log.info(s"Starting InitializeTimer for $initializeTimeout")
    timerScheduler.startSingleTimer(InitializeTimerKey, InitializeTimeout, initializeTimeout)
    component.foreach(ctx.watch)
  }

  private def coordinatedShutdown(): Future[Done] = CoordinatedShutdown(ctx.system.toUntyped).run()
}
