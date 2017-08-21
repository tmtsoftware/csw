package csw.common.framework.internal

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{ActorContext, TimerScheduler}
import akka.typed.{ActorRef, Behavior, Signal, Terminated}
import csw.common.framework.internal.SupervisorMode.Idle
import csw.common.framework.models.CommonSupervisorMsg.{
  ComponentStateSubscription,
  HaltComponent,
  LifecycleStateSubscription
}
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.PreparingToShutdownMsg.{ShutdownComplete, ShutdownFailure, ShutdownTimeout}
import csw.common.framework.models.PubSub.Publish
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart, Shutdown}
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentWiring
import csw.param.states.CurrentState
import csw.services.location.models.ComponentId

import scala.concurrent.duration.{DurationDouble, FiniteDuration}

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
    componentBehaviorFactory: ComponentWiring[_]
) extends MutableBehavior[SupervisorMsg] {

  import Supervisor._

  val name: String                                   = componentInfo.name
  val componentId                                    = ComponentId(name, componentInfo.componentType)
  var haltingFlag                                    = false
  var mode: SupervisorMode                           = Idle
  var runningComponent: Option[ActorRef[RunningMsg]] = None

  val pubSubLifecycle: ActorRef[PubSub[LifecycleStateChanged]] =
    ctx.spawn(PubSubActor.behavior[LifecycleStateChanged], PubSubLifecycleActor)
  val pubSubComponent: ActorRef[PubSub[CurrentState]] =
    ctx.spawn(PubSubActor.behavior[CurrentState], PubSubComponentActor)
  val component: ActorRef[Nothing] =
    ctx.spawn[Nothing](componentBehaviorFactory.compBehavior(componentInfo, ctx.self, pubSubComponent), ComponentActor)

  ctx.watch(component)

  override def onMessage(msg: SupervisorMsg): Behavior[SupervisorMsg] = {
    (mode, msg) match {
      case (_, msg: CommonSupervisorMsg)                                             => onCommonMessages(msg)
      case (SupervisorMode.Idle, msg: SupervisorIdleMsg)                             => onIdleMessages(msg)
      case (SupervisorMode.Running | SupervisorMode.RunningOffline, msg: RunningMsg) => onRunning(msg)
      case (SupervisorMode.PreparingToShutdown, msg: PreparingToShutdownMsg)         => onPreparingToShutdown(msg)
      case (supervisorMode, message) =>
        println(s"Supervisor in $supervisorMode received an unexpected message: $message")
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

  def onCommonMessages(msg: CommonSupervisorMsg): Unit = msg match {
    case LifecycleStateSubscription(subscriberMsg) => pubSubLifecycle ! subscriberMsg
    case ComponentStateSubscription(subscriberMsg) => pubSubComponent ! subscriberMsg
    case HaltComponent                             => haltingFlag = true; handleShutdown()
  }

  def onIdleMessages(msg: SupervisorIdleMsg): Unit = msg match {
    case Initialized(componentRef) =>
      registerWithLocationService()
      componentRef ! Run
    case InitializeFailure(reason) =>
      mode = SupervisorMode.InitializeFailure
    case Running(componentRef) =>
      mode = SupervisorMode.Running
      runningComponent = Some(componentRef)
      pubSubLifecycle ! Publish(LifecycleStateChanged(SupervisorMode.Running, ctx.self))
  }

  def onRunning(msg: RunningMsg): Unit = {
    msg match {
      case Lifecycle(message) => onLifecycle(message)
      case _                  =>
    }
    runningComponent.get ! msg
  }

  def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown => handleShutdown()
    case Restart =>
      mode = SupervisorMode.Idle
      unregisterFromLocationService()
    case GoOffline =>
      if (mode == SupervisorMode.Running) mode = SupervisorMode.RunningOffline
    case GoOnline =>
      if (mode == SupervisorMode.RunningOffline) mode = SupervisorMode.Running
  }

  def onPreparingToShutdown(msg: PreparingToShutdownMsg): Unit = {
    msg match {
      case ShutdownTimeout =>
        mode = SupervisorMode.ShutdownFailure
      case ShutdownFailure(reason) =>
        mode = SupervisorMode.ShutdownFailure
        timerScheduler.cancel(TimerKey)
      case ShutdownComplete =>
        mode = SupervisorMode.Shutdown
        timerScheduler.cancel(TimerKey)
    }
    pubSubLifecycle ! Publish(LifecycleStateChanged(mode, ctx.self))
    if (haltingFlag) haltComponent()
  }

  private def handleShutdown(): Unit = {
    mode = SupervisorMode.PreparingToShutdown
    timerScheduler.startSingleTimer(TimerKey, ShutdownTimeout, shutdownTimeout)
    unregisterFromLocationService()
    pubSubLifecycle ! Publish(LifecycleStateChanged(mode, ctx.self))
  }

  def registerWithLocationService(): Unit = ()

  def unregisterFromLocationService(): Unit = ()

  def haltComponent(): Boolean = {
    ctx.stop(pubSubComponent) & ctx.stop(pubSubLifecycle) & ctx.stop(component)
  }
}
