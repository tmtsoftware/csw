package csw.common.framework.internal.supervisor

import akka.actor.Cancellable
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.internal.PubSubActor
import csw.common.framework.internal.supervisor.SupervisorMode.PreparingToShutdown
import csw.common.framework.models.CommonSupervisorMsg.{
  ComponentStateSubscription,
  HaltComponent,
  LifecycleStateSubscription
}
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.LifecycleState._
import csw.common.framework.models.PreparingToShutdownMsg.{ShutdownComplete, ShutdownFailure, ShutdownTimeout}
import csw.common.framework.models.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
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

  private val shutdownTimeout: FiniteDuration    = 5.seconds
  private var shutdownTimer: Option[Cancellable] = None
  val name: String                               = componentInfo.componentName
  val componentId                                = ComponentId(name, componentInfo.componentType)
  var haltingFlag                                = false
  var lifecycleState: LifecycleState             = LifecycleWaitingForInitialized
  var runningComponent: ActorRef[RunningMsg]     = _
  var mode: SupervisorMode                       = SupervisorMode.Idle
  var isOnline: Boolean                          = false

  val pubSubLifecycle: ActorRef[PubSub[LifecycleStateChanged]] =
    ctx.spawn(PubSubActor.behavior[LifecycleStateChanged], "pub-sub-lifecycle")
  val pubSubComponent: ActorRef[PubSub[CurrentState]] =
    ctx.spawn(PubSubActor.behavior[CurrentState], "pub-sub-component")
  val component: ActorRef[Nothing] =
    ctx.spawn[Nothing](componentBehaviorFactory.compBehavior(componentInfo, ctx.self, pubSubComponent), "component")

  ctx.watch(component)

  override def onMessage(msg: SupervisorMsg): Behavior[SupervisorMsg] = {
    (mode, msg) match {
      case (_, msg: CommonSupervisorMsg)                                     => onCommonMessages(msg)
      case (SupervisorMode.Idle, msg: SupervisorIdleMsg)                     => onIdleMessages(msg)
      case (SupervisorMode.Running, msg: RunningMsg)                         => onRunning(msg)
      case (SupervisorMode.PreparingToShutdown, msg: PreparingToShutdownMsg) => onPreparingToShutdown(msg)
      case (supervisorMode, message) =>
        println(s"Supervisor in $supervisorMode received an unexpected message: $message")
    }
    this
  }

  def onCommonMessages(msg: CommonSupervisorMsg): Unit = msg match {
    case LifecycleStateSubscription(Subscribe(ref))   => pubSubLifecycle ! Subscribe(ref)
    case LifecycleStateSubscription(Unsubscribe(ref)) => pubSubLifecycle ! Unsubscribe(ref)
    case ComponentStateSubscription(Subscribe(ref))   => pubSubComponent ! Subscribe(ref)
    case ComponentStateSubscription(Unsubscribe(ref)) => pubSubComponent ! Unsubscribe(ref)
    case HaltComponent                                => haltingFlag = true; ctx.self ! Lifecycle(Shutdown)
  }

  def onIdleMessages(msg: SupervisorIdleMsg): Unit = msg match {
    case Initialized(componentRef) =>
      registerWithLocationService()
      componentRef ! Run
    case InitializeFailure(reason) =>
      lifecycleState = LifecycleInitializeFailure
      mode = SupervisorMode.LifecycleFailure
    case Running(componentRef) =>
      runningComponent = componentRef
      lifecycleState = LifecycleRunning
      mode = SupervisorMode.Running
      pubSubLifecycle ! Publish(LifecycleStateChanged(LifecycleRunning))
  }

  def onRunning(msg: RunningMsg): Unit = {
    msg match {
      case Lifecycle(message) => onLifecycle(message)
      case _                  =>
    }
    runningComponent ! msg
  }

  def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case Shutdown =>
      shutdownTimer = Some(ctx.schedule(shutdownTimeout, ctx.self, ShutdownTimeout))
      unregisterFromLocationService()
      lifecycleState = LifecyclePreparingToShutdown
      pubSubLifecycle ! Publish(LifecycleStateChanged(LifecyclePreparingToShutdown))
      mode = PreparingToShutdown
    case Restart =>
      unregisterFromLocationService()
      lifecycleState = LifecycleWaitingForInitialized
      mode = SupervisorMode.Idle
    case GoOffline =>
      lifecycleState = LifecycleRunningOffline
      isOnline = false
    case GoOnline =>
      lifecycleState = LifecycleRunning
      isOnline = true
  }

  def onPreparingToShutdown(msg: PreparingToShutdownMsg): Unit = {
    msg match {
      case ShutdownTimeout =>
        lifecycleState = LifecycleShutdownFailure
        mode = SupervisorMode.ShutdownFailure
      case ShutdownFailure(reason) =>
        lifecycleState = LifecycleShutdownFailure
        mode = SupervisorMode.ShutdownFailure
      case ShutdownComplete =>
        shutdownTimer.map(_.cancel())
        lifecycleState = LifecycleShutdown
        mode = SupervisorMode.Shutdown
    }
    pubSubLifecycle ! Publish(LifecycleStateChanged(lifecycleState))
    if (haltingFlag) haltComponent()
  }

  private def registerWithLocationService(): Unit = ()

  private def unregisterFromLocationService(): Unit = ()

  private def haltComponent(): Unit = ()
}
