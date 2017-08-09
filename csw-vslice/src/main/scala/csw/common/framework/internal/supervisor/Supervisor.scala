package csw.common.framework.internal.supervisor

import akka.actor.Cancellable
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.internal.PubSubActor
import csw.common.framework.models.CommonSupervisorMsg.{
  ComponentStateSubscription,
  HaltComponent,
  LifecycleStateSubscription
}
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.PreparingToShutdownMsg.{ShutdownComplete, ShutdownFailure, ShutdownTimeout}
import csw.common.framework.models.PubSub.Publish
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.models.SupervisorMode._
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart, Shutdown}
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentWiring
import csw.param.states.CurrentState
import csw.services.location.models.ComponentId

import scala.concurrent.duration.{DurationDouble, FiniteDuration}

class Supervisor(
    ctx: ActorContext[SupervisorMsg],
    componentInfo: ComponentInfo,
    componentBehaviorFactory: ComponentWiring[_]
) extends MutableBehavior[SupervisorMsg] {

  private val shutdownTimeout: FiniteDuration        = 5.seconds
  private var shutdownTimer: Option[Cancellable]     = None
  val name: String                                   = componentInfo.componentName
  val componentId                                    = ComponentId(name, componentInfo.componentType)
  var haltingFlag                                    = false
  var mode: SupervisorMode                           = Idle
  var runningComponent: Option[ActorRef[RunningMsg]] = None

  val pubSubLifecycle: ActorRef[PubSub[LifecycleStateChanged]] =
    ctx.spawn(PubSubActor.behavior[LifecycleStateChanged], "pub-sub-lifecycle")
  val pubSubComponent: ActorRef[PubSub[CurrentState]] =
    ctx.spawn(PubSubActor.behavior[CurrentState], "pub-sub-component")
  val component: ActorRef[Nothing] =
    ctx.spawn[Nothing](componentBehaviorFactory.compBehavior(componentInfo, ctx.self, pubSubComponent), "component")

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

  def onCommonMessages(msg: CommonSupervisorMsg): Unit = msg match {
    case LifecycleStateSubscription(subscriberMsg) => pubSubLifecycle ! subscriberMsg
    case ComponentStateSubscription(subscriberMsg) => pubSubComponent ! subscriberMsg
    case HaltComponent                             => haltingFlag = true; ctx.self ! Lifecycle(Shutdown)
  }

  def onIdleMessages(msg: SupervisorIdleMsg): Unit = msg match {
    case Initialized(componentRef) =>
      registerWithLocationService()
      componentRef ! Run
    case InitializeFailure(reason) =>
      mode = SupervisorMode.InitializeFailure
    case Running(componentRef) =>
      runningComponent = Some(componentRef)
      mode = SupervisorMode.Running
      pubSubLifecycle ! Publish(LifecycleStateChanged(SupervisorMode.Running))
  }

  def onRunning(msg: RunningMsg): Unit = {
    msg match {
      case Lifecycle(message) => onLifecycle(message)
      case _                  =>
    }
    runningComponent.get ! msg
  }

  def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown =>
      shutdownTimer = Some(ctx.schedule(shutdownTimeout, ctx.self, ShutdownTimeout))
      unregisterFromLocationService()
      mode = SupervisorMode.PreparingToShutdown
      pubSubLifecycle ! Publish(LifecycleStateChanged(mode))
    case Restart =>
      unregisterFromLocationService()
      mode = SupervisorMode.Idle
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
      case ShutdownComplete =>
        shutdownTimer.map(_.cancel())
        mode = SupervisorMode.Shutdown
    }
    pubSubLifecycle ! Publish(LifecycleStateChanged(mode))
    if (haltingFlag) haltComponent()
  }

  def registerWithLocationService(): Unit = ()

  def unregisterFromLocationService(): Unit = ()

  def haltComponent(): Boolean = {
    ctx.stop(component) && ctx.stop(pubSubComponent) && ctx.stop(pubSubLifecycle)
  }
}
