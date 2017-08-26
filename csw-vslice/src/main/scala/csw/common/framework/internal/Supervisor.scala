package csw.common.framework.internal

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{ActorContext, TimerScheduler}
import akka.typed.{ActorRef, Behavior, Signal, Terminated}
import csw.common.framework.internal.SupervisorMode.Idle
import csw.common.framework.models.SupervisorCommonMsg.{
  ComponentStateSubscription,
  GetSupervisorMode,
  HaltComponent,
  LifecycleStateSubscription
}
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.ModeMsg.SupervisorModeMsg
import csw.common.framework.models.PreparingToShutdownMsg.{ShutdownComplete, ShutdownFailure, ShutdownTimeout}
import csw.common.framework.models.PubSub.Publish
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.SupervisorIdleComponentMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.models.SupervisorIdleMessage.{RegistrationComplete, RegistrationFailed}
import csw.common.framework.models.SupervisorRunningMessage.{UnRegistrationComplete, UnRegistrationFailed}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart, Shutdown}
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentWiring
import csw.param.states.CurrentState
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, RegistrationResult}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.{Failure, Success}

object Supervisor {
  val PubSubComponentActor            = "pub-sub-component"
  val ComponentActor                  = "component"
  val PubSubLifecycleActor            = "pub-sub-lifecycle"
  val TimerKey                        = "shutdown-timer"
  val shutdownTimeout: FiniteDuration = 5.seconds
}

class Supervisor(
    ctx: ActorContext[SupervisorMsg],
    timerScheduler: TimerScheduler[SupervisorMsg],
    componentInfo: ComponentInfo,
    componentBehaviorFactory: ComponentWiring[_],
    registrationFactory: RegistrationFactory,
    locationService: LocationService
) extends MutableBehavior[SupervisorMsg] {

  import Supervisor._

  implicit val ec: ExecutionContext = ctx.executionContext

  val name: String                                   = componentInfo.name
  val componentId                                    = ComponentId(name, componentInfo.componentType)
  val akkaRegistration: AkkaRegistration             = registrationFactory.akkaTyped(AkkaConnection(componentId), ctx.self)
  var haltingFlag                                    = false
  var mode: SupervisorMode                           = Idle
  var runningComponent: Option[ActorRef[RunningMsg]] = None
  var registrationOpt: Option[RegistrationResult]    = None

  val pubSubLifecycle: ActorRef[PubSub[LifecycleStateChanged]] =
    ctx.spawn(PubSubActor.behavior[LifecycleStateChanged], PubSubLifecycleActor)
  val pubSubComponent: ActorRef[PubSub[CurrentState]] =
    ctx.spawn(PubSubActor.behavior[CurrentState], PubSubComponentActor)
  val component: ActorRef[Nothing] =
    ctx.spawn[Nothing](componentBehaviorFactory.compBehavior(componentInfo, ctx.self, pubSubComponent), ComponentActor)

  ctx.watch(component)

  override def onMessage(msg: SupervisorMsg): Behavior[SupervisorMsg] = {
    (mode, msg) match {
      case (_, msg: SupervisorCommonMsg)                                                           ⇒ onCommon(msg)
      case (SupervisorMode.Idle, msg: SupervisorIdleMessage)                                       ⇒ onIdle(msg)
      case (SupervisorMode.Running | SupervisorMode.RunningOffline, msg: SupervisorRunningMessage) ⇒ onRunning(msg)
      case (SupervisorMode.PreparingToShutdown, msg: PreparingToShutdownMsg)                       ⇒ onPreparingToShutdown(msg)
      case (_, message) =>
        println(s"Supervisor in $mode received an unexpected message: $message")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[SupervisorMsg]] = {
    case Terminated(componentRef) ⇒
      ctx.unwatch(component)
      unregisterFromLocationService()
      haltComponent()
      Behavior.stopped
  }

  def onCommon(msg: SupervisorCommonMsg): Unit = msg match {
    case LifecycleStateSubscription(subscriberMsg) ⇒ pubSubLifecycle ! subscriberMsg
    case ComponentStateSubscription(subscriberMsg) ⇒ pubSubComponent ! subscriberMsg
    case HaltComponent                             ⇒ haltingFlag = true; handleShutdown()
    case GetSupervisorMode(replyTo)                ⇒ replyTo ! SupervisorModeMsg(ctx.self, mode)
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
      pubSubLifecycle ! Publish(LifecycleStateChanged(ctx.self, SupervisorMode.Running))
  }

  def onRunning(msg: SupervisorRunningMessage): Unit = {
    msg match {
      case runningMsg: RunningMsg          ⇒ handleRunningMsg(runningMsg)
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

  def onPreparingToShutdown(msg: PreparingToShutdownMsg): Unit = {
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

  private def handleRunningMsg(runningMsg: RunningMsg): Unit = {
    runningMsg match {
      case Lifecycle(message) ⇒ onLifecycle(message)
      case _                  ⇒
    }
    runningComponent.get ! runningMsg
  }

  private def handleShutdown(): Unit = {
    unregisterFromLocationService()
    mode = SupervisorMode.PreparingToShutdown
    timerScheduler.startSingleTimer(TimerKey, ShutdownTimeout, shutdownTimeout)
    pubSubLifecycle ! Publish(LifecycleStateChanged(ctx.self, mode))
  }

  private def registerWithLocationService(componentRef: ActorRef[InitialMsg]): Unit = {
    locationService.register(akkaRegistration).onComplete {
      case Success(registrationResult) ⇒ ctx.self ! RegistrationComplete(registrationResult, componentRef)
      case Failure(throwable)          ⇒ ctx.self ! RegistrationFailed(throwable)
    }
  }

  private def onRegistrationComplete(registrationResult: RegistrationResult,
                                     componentRef: ActorRef[InitialMsg]): Unit = {
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
