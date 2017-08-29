package csw.common.framework.internal.supervisor

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{ActorContext, TimerScheduler}
import akka.typed.{ActorRef, Behavior, Signal, Terminated}
import csw.common.framework.internal.PubSubActor
import csw.common.framework.internal.supervisor.SupervisorMode.Idle
import csw.common.framework.models.ContainerIdleMessage.SupervisorModeChanged
import csw.common.framework.models.InitialMessage.Run
import csw.common.framework.models.PreparingToShutdownMessage.{ShutdownComplete, ShutdownFailure, ShutdownTimeout}
import csw.common.framework.models.PubSub.Publish
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorCommonMessage.{
  ComponentStateSubscription,
  GetSupervisorMode,
  HaltComponent,
  LifecycleStateSubscription
}
import csw.common.framework.models.SupervisorIdleComponentMessage.{InitializeFailure, Initialized, Running}
import csw.common.framework.models.SupervisorIdleMessage.{RegistrationComplete, RegistrationFailed}
import csw.common.framework.models.SupervisorRunningMessage.{UnRegistrationComplete, UnRegistrationFailed}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart, Shutdown}
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentBehaviorFactory
import csw.param.states.CurrentState
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, RegistrationResult}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.{Failure, Success}

object SupervisorBehavior {
  val PubSubComponentActor            = "pub-sub-component"
  val ComponentActor                  = "component"
  val PubSubLifecycleActor            = "pub-sub-lifecycle"
  val TimerKey                        = "shutdown-timer"
  val shutdownTimeout: FiniteDuration = 5.seconds
}

class SupervisorBehavior(
    ctx: ActorContext[SupervisorMessage],
    maybeContainerRef: Option[ActorRef[ContainerIdleMessage]],
    timerScheduler: TimerScheduler[SupervisorMessage],
    componentInfo: ComponentInfo,
    componentBehaviorFactory: ComponentBehaviorFactory[_],
    registrationFactory: RegistrationFactory,
    locationService: LocationService
) extends MutableBehavior[SupervisorMessage] {

  import SupervisorBehavior._

  implicit val ec: ExecutionContext = ctx.executionContext

  val name: String                                       = componentInfo.name
  val componentId                                        = ComponentId(name, componentInfo.componentType)
  val akkaRegistration: AkkaRegistration                 = registrationFactory.akkaTyped(AkkaConnection(componentId), ctx.self)
  var haltingFlag                                        = false
  var mode: SupervisorMode                               = Idle
  var runningComponent: Option[ActorRef[RunningMessage]] = None
  var registrationOpt: Option[RegistrationResult]        = None

  val pubSubLifecycle: ActorRef[PubSub[LifecycleStateChanged]] =
    ctx.spawn(PubSubActor.behavior[LifecycleStateChanged], PubSubLifecycleActor)
  val pubSubComponent: ActorRef[PubSub[CurrentState]] =
    ctx.spawn(PubSubActor.behavior[CurrentState], PubSubComponentActor)
  val component: ActorRef[Nothing] =
    ctx.spawn[Nothing](componentBehaviorFactory.make(componentInfo, ctx.self, pubSubComponent), ComponentActor)

  ctx.watch(component)

  override def onMessage(msg: SupervisorMessage): Behavior[SupervisorMessage] = {
    (mode, msg) match {
      case (_, msg: SupervisorCommonMessage)                                                       ⇒ onCommon(msg)
      case (SupervisorMode.Idle, msg: SupervisorIdleMessage)                                       ⇒ onIdle(msg)
      case (SupervisorMode.Running | SupervisorMode.RunningOffline, msg: SupervisorRunningMessage) ⇒ onRunning(msg)
      case (SupervisorMode.PreparingToShutdown, msg: PreparingToShutdownMessage)                   ⇒ onPreparingToShutdown(msg)
      case (_, message) =>
        println(s"Supervisor in $mode received an unexpected message: $message")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[SupervisorMessage]] = {
    case Terminated(componentRef) ⇒
      ctx.unwatch(component)
      unregisterFromLocationService()
      haltComponent()
      Behavior.stopped
  }

  def onCommon(msg: SupervisorCommonMessage): Unit = msg match {
    case LifecycleStateSubscription(subscriberMessage) ⇒ pubSubLifecycle ! subscriberMessage
    case ComponentStateSubscription(subscriberMessage) ⇒ pubSubComponent ! subscriberMessage
    case HaltComponent                                 ⇒ haltingFlag = true; handleShutdown()
    case GetSupervisorMode(replyTo)                    ⇒ replyTo ! mode
  }

  def onIdle(msg: SupervisorIdleMessage): Unit = msg match {
    case Initialized(componentRef) ⇒
      registerWithLocationService(componentRef)
    case InitializeFailure(reason) ⇒
      mode = SupervisorMode.InitializeFailure
    case RegistrationComplete(registrationResult, componentRef) ⇒
      onRegistrationComplete(registrationResult, componentRef)
    case RegistrationFailed(throwable) ⇒
      onRegistrationFailed(throwable)
    case Running(componentRef) ⇒
      mode = SupervisorMode.Running
      runningComponent = Some(componentRef)
      maybeContainerRef foreach (_ ! SupervisorModeChanged(ctx.self, mode))
      pubSubLifecycle ! Publish(LifecycleStateChanged(ctx.self, SupervisorMode.Running))
  }

  def onRunning(msg: SupervisorRunningMessage): Unit = {
    msg match {
      case runningMessage: RunningMessage  ⇒ handleRunningMessage(runningMessage)
      case UnRegistrationComplete          ⇒ onUnRegistrationComplete()
      case UnRegistrationFailed(throwable) ⇒ onUnRegistrationFailed(throwable)
    }
  }

  def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown ⇒ handleShutdown()
    case Restart ⇒
      mode = SupervisorMode.Idle
      unregisterFromLocationService()
    case GoOffline ⇒
      if (mode == SupervisorMode.Running) mode = SupervisorMode.RunningOffline
    case GoOnline ⇒
      if (mode == SupervisorMode.RunningOffline) mode = SupervisorMode.Running
  }

  def onPreparingToShutdown(msg: PreparingToShutdownMessage): Unit = {
    msg match {
      case ShutdownTimeout ⇒
        mode = SupervisorMode.ShutdownFailure
      case ShutdownFailure(reason) ⇒
        mode = SupervisorMode.ShutdownFailure
        timerScheduler.cancel(TimerKey)
      case ShutdownComplete ⇒
        mode = SupervisorMode.Shutdown
        timerScheduler.cancel(TimerKey)
    }
    pubSubLifecycle ! Publish(LifecycleStateChanged(ctx.self, mode))
    if (haltingFlag) haltComponent()
  }

  def haltComponent(): Boolean = {
    ctx.stop(pubSubComponent) & ctx.stop(pubSubLifecycle) & ctx.stop(component)
  }

  private def handleRunningMessage(runningMessage: RunningMessage): Unit = {
    runningMessage match {
      case Lifecycle(message) ⇒ onLifecycle(message)
      case _                  ⇒
    }
    runningComponent.get ! runningMessage
  }

  private def handleShutdown(): Unit = {
    mode = SupervisorMode.PreparingToShutdown
    pubSubLifecycle ! Publish(LifecycleStateChanged(ctx.self, mode))
    unregisterFromLocationService()
    timerScheduler.startSingleTimer(TimerKey, ShutdownTimeout, shutdownTimeout)
  }

  private def registerWithLocationService(componentRef: ActorRef[InitialMessage]): Unit = {
    locationService.register(akkaRegistration).onComplete {
      case Success(registrationResult) ⇒ ctx.self ! RegistrationComplete(registrationResult, componentRef)
      case Failure(throwable)          ⇒ ctx.self ! RegistrationFailed(throwable)
    }
  }

  private def onRegistrationComplete(registrationResult: RegistrationResult,
                                     componentRef: ActorRef[InitialMessage]): Unit = {
    registrationOpt = Some(registrationResult)
    componentRef ! Run
  }

  private def onRegistrationFailed(throwable: Throwable): Unit =
    println(s"log.error($throwable)") //FIXME use log statement

  private def unregisterFromLocationService(): Unit = {
    registrationOpt match {
      case Some(registrationResult) ⇒
        registrationResult.unregister().onComplete {
          case Success(_)         ⇒ ctx.self ! UnRegistrationComplete
          case Failure(throwable) ⇒ ctx.self ! UnRegistrationFailed(throwable)
        }
      case None ⇒
        println("log.warn(No valid RegistrationResult found to unregister.)") //FIXME to log error
    }
  }

  private def onUnRegistrationComplete(): Unit = ???

  private def onUnRegistrationFailed(throwable: Throwable): Unit = ???
}
