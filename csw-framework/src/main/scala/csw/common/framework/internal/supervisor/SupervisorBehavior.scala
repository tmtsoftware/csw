package csw.common.framework.internal.supervisor

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.scaladsl.{Actor, ActorContext, TimerScheduler}
import akka.typed.{ActorRef, Behavior, PostStop, Signal, SupervisorStrategy, Terminated}
import csw.common.framework.exceptions.FailureRestart
import csw.common.framework.internal.pubsub.PubSubBehaviorFactory
import csw.common.framework.internal.supervisor.SupervisorMode.Idle
import csw.common.framework.models.FromComponentLifecycleMessage.Running
import csw.common.framework.models.FromSupervisorMessage.SupervisorModeChanged
import csw.common.framework.models.PubSub.Publish
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorCommonMessage.{
  ComponentStateSubscription,
  GetSupervisorMode,
  LifecycleStateSubscription
}
import csw.common.framework.models.SupervisorIdleMessage._
import csw.common.framework.models.SupervisorRestartMessage.{UnRegistrationComplete, UnRegistrationFailed}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentBehaviorFactory
import csw.param.states.CurrentState
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, RegistrationResult}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.{Failure, Success}

object SupervisorBehavior {
  val PubSubComponentActor              = "pub-sub-component"
  val ComponentActor                    = "component"
  val PubSubLifecycleActor              = "pub-sub-lifecycle"
  val InitializeTimerKey                = "initialize-timer"
  val initializeTimeout: FiniteDuration = 5.seconds
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
) extends ComponentLogger.TypedActor[SupervisorMessage](ctx, componentInfo.name) {

  import SupervisorBehavior._

  implicit val ec: ExecutionContext = ctx.executionContext

  val name: String                                       = componentInfo.name
  val componentId                                        = ComponentId(name, componentInfo.componentType)
  val akkaRegistration: AkkaRegistration                 = registrationFactory.akkaTyped(AkkaConnection(componentId), ctx.self)
  var haltingFlag                                        = false
  var mode: SupervisorMode                               = Idle
  var runningComponent: Option[ActorRef[RunningMessage]] = None
  var registrationOpt: Option[RegistrationResult]        = None
  var component: ActorRef[Nothing]                       = _

  val pubSubLifecycle: ActorRef[PubSub[LifecycleStateChanged]] = pubSubBehaviorFactory.make(ctx, PubSubLifecycleActor)
  val pubSubComponent: ActorRef[PubSub[CurrentState]]          = pubSubBehaviorFactory.make(ctx, PubSubComponentActor)

  registerWithLocationService()

  override def onMessage(msg: SupervisorMessage): Behavior[SupervisorMessage] = {
    log.debug(s"Supervisor in mode :[$mode] received message :[$msg]")
    (mode, msg) match {
      case (_, msg: SupervisorCommonMessage)                                                       ⇒ onCommon(msg)
      case (SupervisorMode.Idle, msg: SupervisorIdleMessage)                                       ⇒ onIdle(msg)
      case (SupervisorMode.Running | SupervisorMode.RunningOffline, msg: SupervisorRunningMessage) ⇒ onRunning(msg)
      case (SupervisorMode.Restart, msg: SupervisorRestartMessage)                                 ⇒ onRestarting(msg)
      case (_, message) =>
        log.error(s"Unexpected message :[$message] received by supervisor in mode :[$mode]")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[SupervisorMessage]] = {
    case Terminated(componentRef) ⇒
      mode match {
        case SupervisorMode.Restart ⇒
          log.warn(s"Supervisor in mode :[$mode] received terminated signal from component :[$componentRef]")
          mode = SupervisorMode.Idle
          registerWithLocationService()
        case SupervisorMode.Shutdown ⇒
          log.warn(s"Supervisor in mode :[$mode] received terminated signal from component :[$componentRef]")
          coordinatedShutdown()
        case _ ⇒
          log.error(
            s"Supervisor in mode :[$mode] received unexpected terminated signal from component :[$componentRef]"
          )
      }
      this
    case PostStop ⇒
      log.warn(s"Supervisor is shutting down. Un-registering supervisor from location service")
      registrationOpt.foreach(registrationResult ⇒ registrationResult.unregister())
      this
  }

  def onCommon(msg: SupervisorCommonMessage): Unit = msg match {
    case LifecycleStateSubscription(subscriberMessage) ⇒ pubSubLifecycle ! subscriberMessage
    case ComponentStateSubscription(subscriberMessage) ⇒ pubSubComponent ! subscriberMessage
    case GetSupervisorMode(replyTo)                    ⇒ replyTo ! mode
    case Restart                                       ⇒ onRestart()
    case Shutdown ⇒
      log.debug(s"Supervisor is changing state from [$mode] to ${SupervisorMode.Shutdown}")
      mode = SupervisorMode.Shutdown
      ctx.stop(component)
  }

  def onIdle(msg: SupervisorIdleMessage): Unit = msg match {
    case RegistrationComplete(registrationResult) ⇒
      log.info(s"Supervisor with connection :[${akkaRegistration.connection}] is registering with location service")
      registrationOpt = Some(registrationResult)
      spawnAndWatchComponent()
    case RegistrationFailed(throwable) ⇒
      log.error(throwable.getMessage, ex = throwable)
      throw throwable
    case Running(componentRef) ⇒
      log.debug(s"Supervisor is changing state from [$mode] to ${SupervisorMode.Running}")
      mode = SupervisorMode.Running
      timerScheduler.cancel(InitializeTimerKey)
      runningComponent = Some(componentRef)
      maybeContainerRef foreach { container ⇒
        container ! SupervisorModeChanged(ctx.self, mode)
        log.debug(s"Supervisor acknowledged container :[$container] for mode :[$mode]")
      }
      pubSubLifecycle ! Publish(LifecycleStateChanged(ctx.self, SupervisorMode.Running))
    case InitializeTimeout ⇒
      log.error("Component initialization timed out")
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
    log.debug(s"Supervisor is changing state from [$mode] to ${SupervisorMode.Restart}")
    mode = SupervisorMode.Restart
    registrationOpt match {
      case Some(registrationResult) ⇒
        unRegisterFromLocationService(registrationResult)
      case None ⇒
        log.warn("No valid RegistrationResult found to unregister")
        respawnComponent()
    }
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
    ctx.child(ComponentActor) match {
      case Some(componentRef) ⇒
        ctx.stop(component)
      case None ⇒
        log.debug(s"Supervisor is changing state from [$mode] to [${SupervisorMode.Idle}]")
        mode = SupervisorMode.Idle
        registerWithLocationService()
    }
  }

  private def onLifeCycle(message: ToComponentLifecycleMessage): Unit = {
    message match {
      case GoOffline ⇒
        if (mode == SupervisorMode.Running) {
          log.debug(s"Supervisor is changing state from [$mode] to [${SupervisorMode.RunningOffline}]")
          mode = SupervisorMode.RunningOffline
        }
      case GoOnline ⇒
        if (mode == SupervisorMode.RunningOffline) {
          log.debug(s"Supervisor is changing state from [$mode] to [${SupervisorMode.Running}]")
          mode = SupervisorMode.Running
        }
    }
  }

  private def registerWithLocationService(): Unit = {
    log.debug(s"Supervisor with connection :[${akkaRegistration.connection}] is registering with location service")
    locationService.register(akkaRegistration).onComplete {
      case Success(registrationResult) ⇒ ctx.self ! RegistrationComplete(registrationResult)
      case Failure(throwable)          ⇒ ctx.self ! RegistrationFailed(throwable)
    }
  }

  private def unRegisterFromLocationService(registrationResult: RegistrationResult): Unit = {
    log.debug(s"Un-registering supervisor from location service")
    registrationResult.unregister().onComplete {
      case Success(_)         ⇒ ctx.self ! UnRegistrationComplete
      case Failure(throwable) ⇒ ctx.self ! UnRegistrationFailed(throwable)
    }
  }

  private def spawnAndWatchComponent(): Unit = {
    log.debug(s"Supervisor is spawning component")
    component = ctx.spawn[Nothing](
      Actor
        .supervise[Nothing](componentBehaviorFactory.make(componentInfo, ctx.self, pubSubComponent, locationService))
        .onFailure[FailureRestart](SupervisorStrategy.restart.withLoggingEnabled(true)),
      ComponentActor
    )
    ctx.watch(component)
    timerScheduler.startSingleTimer(InitializeTimerKey, InitializeTimeout, initializeTimeout)
  }

  private def coordinatedShutdown(): Future[Done] = CoordinatedShutdown(ctx.system.toUntyped).run()
}
