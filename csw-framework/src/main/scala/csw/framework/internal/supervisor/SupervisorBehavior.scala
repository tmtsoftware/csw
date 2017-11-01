package csw.framework.internal.supervisor

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.scaladsl.{Actor, ActorContext, TimerScheduler}
import akka.typed.{ActorRef, Behavior, PostStop, Signal, SupervisorStrategy, Terminated}
import csw.exceptions.{FailureRestart, InitializationFailed}
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.scaladsl.ComponentBehaviorFactory
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.FromSupervisorMessage.SupervisorLifecycleStateChanged
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
import csw.messages.framework.LocationServiceUsage.DoNotRegister
import csw.messages.framework.SupervisorLifecycleState.Idle
import csw.messages.framework.{ComponentInfo, SupervisorLifecycleState}
import csw.messages.location.ComponentId
import csw.messages.location.Connection.AkkaConnection
import csw.messages.params.states.CurrentState
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
  val ComponentActorNameSuffix = "component-actor"
}

/**
 * The Behavior of a Supervisor of a component actor represented as a mutable behavior.
 *
 * @param ctx                        The Actor Context under which the actor instance of this behavior is created
 * @param timerScheduler             Provides support for scheduled `self` messages in an actor
 * @param maybeContainerRef          The container ref of the container under which this supervisor is started if
 *                                  its not running in standalone mode
 * @param componentInfo              ComponentInfo as described in the configuration file
 * @param componentBehaviorFactory   The factory for creating the component supervised by this Supervisor
 * @param pubSubBehaviorFactory      The factory for creating actor instance of [[csw.framework.internal.pubsub.PubSubBehavior]]
 *                                  for utilising pub-sub of any state of a component
 * @param registrationFactory        The factory for creating a typed [[AkkaRegistration]] from [[AkkaConnection]]
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
) extends ComponentLogger.MutableActor[SupervisorMessage](ctx, componentInfo.name) {

  import SupervisorBehavior._
  import ctx.executionContext

  val componentName: String              = componentInfo.name
  val componentActorName                 = s"$componentName-$ComponentActorNameSuffix"
  val initializeTimeout: FiniteDuration  = componentInfo.initializeTimeout
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

  /**
   * Defines processing for a [[SupervisorMessage]] received by the actor instance.
   * @param msg      SupervisorMessage received
   * @return         The existing behavior
   */
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

  /**
   * Defines processing for a [[akka.typed.Signal]] received by the actor instance.
   * @return        The existing behavior
   */
  override def onSignal: PartialFunction[Signal, Behavior[SupervisorMessage]] = {
    case Terminated(componentRef) ⇒
      log.warn(
        s"Supervisor in lifecycle state :[$lifecycleState] received terminated signal from component :[$componentRef]"
      )
      timerScheduler.cancel(InitializeTimerKey)
      lifecycleState match {
        case SupervisorLifecycleState.Restart  ⇒ spawn()
        case SupervisorLifecycleState.Shutdown ⇒ coordinatedShutdown()
        case SupervisorLifecycleState.Idle     ⇒ if (isStandalone) throw InitializationFailed
        case _ ⇒
          lifecycleState = SupervisorLifecycleState.Idle // Change mode to idle and wait for an action(Restart or Shutdown) to be taken from outside
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
    case Shutdown ⇒
      log.debug(
        s"Supervisor is changing lifecycle state from [$lifecycleState] to [${SupervisorLifecycleState.Shutdown}]"
      )
      lifecycleState = SupervisorLifecycleState.Shutdown
      ctx.child(componentActorName) match {
        case Some(componentRef) ⇒
          ctx.stop(componentRef) // stop component actor for a graceful shutdown before shutting down the actor system
        case None ⇒ coordinatedShutdown()
      }
  }

  /**
   * Defines action for messages which can be received in [[SupervisorLifecycleState.Idle]] state
   * @param idleMessage  Message representing a message received in [[SupervisorLifecycleState.Idle]] state
   */
  private def onIdle(idleMessage: SupervisorIdleMessage): Unit = idleMessage match {
    case RegistrationSuccess(componentRef) ⇒
      onRegistrationComplete(componentRef)
    case RegistrationNotRequired(componentRef) ⇒
      onRegistrationComplete(componentRef)
    case RegistrationFailed(throwable) ⇒
      log.error(throwable.getMessage, ex = throwable)
      throw throwable
    case Running(componentRef) ⇒
      log.info("Received Running message from component within timeout, cancelling InitializeTimer")
      timerScheduler.cancel(InitializeTimerKey)
      log.debug(
        s"Supervisor is changing lifecycle state from [$lifecycleState] to [${SupervisorLifecycleState.Running}]"
      )
      registerWithLocationService(componentRef)
    case InitializeTimeout ⇒
      log.error("Component TLA initialization timed out")
      component.foreach(ctx.stop)
  }

  /**
   * Defines action for messages which can be received in [[SupervisorLifecycleState.Running]] state
   * @param runningMessage  Message representing a message received in [[SupervisorLifecycleState.Running]] state
   */
  private def onRunning(runningMessage: SupervisorRunningMessage): Unit = {
    runningMessage match {
      case runningMessage: RunningMessage ⇒
        runningMessage match {
          case Lifecycle(message) ⇒ onLifeCycle(message)
          case _                  ⇒
        }
        runningComponent.get ! runningMessage
    }
  }

  /**
   * Defines action for messages which can be received in [[SupervisorLifecycleState.Restart]] state
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

  private def onRestart(): Unit = {
    log.debug(s"Supervisor is changing lifecycle state from [$lifecycleState] to [${SupervisorLifecycleState.Restart}]")
    lifecycleState = SupervisorLifecycleState.Restart
    unRegisterFromLocationService()
  }

  private def respawnComponent(): Unit = {
    log.info("Supervisor re-spawning component")
    ctx.child(componentActorName) match {
      case Some(_) ⇒ component.foreach(ctx.stop) // stop component actor for a graceful shutdown before restart
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

  private def registerWithLocationService(componentRef: ActorRef[RunningMessage]): Unit = {
    //Honour DoNotRegister received in componentInfo
    if (componentInfo.locationServiceUsage == DoNotRegister)
      ctx.self ! RegistrationNotRequired(componentRef)
    else {
      locationService.register(akkaRegistration).onComplete {
        case Success(_)         ⇒ ctx.self ! RegistrationSuccess(componentRef)
        case Failure(throwable) ⇒ ctx.self ! RegistrationFailed(throwable)
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

  private def onRegistrationComplete(componentRef: ActorRef[RunningMessage]): Unit = {
    lifecycleState = SupervisorLifecycleState.Running
    runningComponent = Some(componentRef)
    maybeContainerRef foreach { container ⇒
      container ! SupervisorLifecycleStateChanged(ctx.self, lifecycleState)
      log.debug(s"Supervisor acknowledged container :[$container] for lifecycle state :[$lifecycleState]")
    }
    pubSubLifecycle ! Publish(LifecycleStateChanged(ctx.self, SupervisorLifecycleState.Running))
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
    // watch created component to get notified when it’s terminated.
    component.foreach(ctx.watch)
  }

  /**
   * Trigger actor system shutdown with graceful exit from the cluster
   * @return
   */
  private def coordinatedShutdown(): Future[Done] = CoordinatedShutdown(ctx.system.toUntyped).run()
}
