package csw.common.framework.scaladsl.supervisor

import akka.actor.Cancellable
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.CommonSupervisorMsg.{
  HaltComponent,
  SubscribeLifecycleCallback,
  UnsubscribeLifecycleCallback
}
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.LifecycleState._
import csw.common.framework.models.PreparingToShutdownMsg.{ShutdownComplete, ShutdownFailure, ShutdownTimeout}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.SupervisorExternalMsg._
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart}
import csw.common.framework.models._
import csw.common.framework.scaladsl.ComponentBehaviorFactory
import csw.common.framework.scaladsl.supervisor.SupervisorMode.{Idle, PreparingToShutdown}
import csw.services.location.models.ComponentId

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

class Supervisor[CompInfo <: ComponentInfo](ctx: ActorContext[SupervisorMsg],
                                            componentInfo: CompInfo,
                                            componentBehaviorFactory: ComponentBehaviorFactory[CompInfo])
    extends MutableBehavior[SupervisorMsg] {

  implicit val ec: ExecutionContextExecutor                   = ctx.executionContext
  private val shutdownTimeout: FiniteDuration                 = 5.seconds
  private var shutdownTimer: Option[Cancellable]              = None
  private val name                                            = componentInfo.componentName
  private val componentId                                     = ComponentId(name, componentInfo.componentType)
  private var haltingFlag                                     = false
  var lifecycleState: LifecycleState                          = LifecycleWaitingForInitialized
  var runningComponent: ActorRef[RunningMsg]                  = _
  var mode: SupervisorMode                                    = Idle
  var isOnline: Boolean                                       = false
  private var listeners: Set[ActorRef[LifecycleStateChanged]] = Set[ActorRef[LifecycleStateChanged]]()

  val component: ActorRef[Nothing] =
    ctx.spawnAnonymous[Nothing](componentBehaviorFactory.behavior(componentInfo, ctx.self))

  ctx.watch(component)

  override def onMessage(msg: SupervisorMsg): Behavior[SupervisorMsg] = {
    (mode, msg) match {
      case (SupervisorMode.Idle, msg: SupervisorIdleMsg)                     => onIdleMessages(msg)
      case (SupervisorMode.Idle, msg: CommonSupervisorMsg)                   => onCommonMessages(msg)
      case (SupervisorMode.Running, msg: SupervisorExternalMsg)              => onRunning(msg)
      case (SupervisorMode.Running, msg: CommonSupervisorMsg)                => onCommonMessages(msg)
      case (SupervisorMode.PreparingToShutdown, msg: PreparingToShutdownMsg) => onPreparingToShutdown(msg)
      case (SupervisorMode.Shutdown, msg: CommonSupervisorMsg)               => onCommonMessages(msg)
      case (SupervisorMode.Shutdown, x)                                      => onShutdown(x)
      case (SupervisorMode.ShutdownFailure, x)                               => onShutdownfailure(x)
      case (SupervisorMode.LifecycleFailure, x)                              => onLifecycleFailure(x)
    }
    this
  }

  def onIdleMessages(msg: SupervisorIdleMsg): Unit = msg match {
    case Initialized(componentRef) => onInitialized(componentRef)
    case InitializeFailure(reason) => onInitializeFailure(reason)
    case Running(componentRef)     => onRunningComponent(componentRef)
  }

  def onCommonMessages(msg: CommonSupervisorMsg): Unit = msg match {
    case SubscribeLifecycleCallback(actorRef)   => addListener(actorRef)
    case UnsubscribeLifecycleCallback(actorRef) => removeListener(actorRef)
    case HaltComponent                          => onHalt()
  }

  def onRunning(msg: SupervisorExternalMsg): Unit = msg match {
    case ExComponentRestart  => onExComponentRestart()
    case ExComponentShutdown => onExcomponentShutDown()
    case ExComponentOnline   => onExcomponentOnline()
    case ExComponentOffline  => onExcomponentOffline()
  }

  def onExComponentRestart(): Unit = {
    runningComponent ! Lifecycle(Restart)
    unregisterFromLocationService()
    lifecycleState = LifecycleWaitingForInitialized
    mode = SupervisorMode.Idle
  }

  private def onExcomponentShutDown(): Unit = {
    shutdownTimer = Some(scheduleTimeout)
    runningComponent ! Lifecycle(ToComponentLifecycleMessage.Shutdown)
    unregisterFromLocationService()
    lifecycleState = LifecyclePreparingToShutdown
    notifyListeners(LifecycleStateChanged(LifecyclePreparingToShutdown))
    mode = PreparingToShutdown
  }

  def onExcomponentOnline(): Unit = if (!isOnline) {
    runningComponent ! Lifecycle(GoOnline)
    lifecycleState = LifecycleRunning
    isOnline = true
  }

  def onExcomponentOffline(): Unit = if (isOnline) {
    runningComponent ! Lifecycle(GoOffline)
    lifecycleState = LifecycleRunningOffline
    isOnline = false
  }

  def onPreparingToShutdown(msg: PreparingToShutdownMsg): Unit = msg match {
    case ShutdownTimeout         => onShutdownTimeOut()
    case ShutdownFailure(reason) => onShutdownFailure(reason)
    case ShutdownComplete        => onShutdownComplete()
  }

  private def onLifecycleFailure(x: SupervisorMsg): Unit = {
    println(s"Supervisor in Lifecycle Failure received an unexpected message: $x ")
  }

  private def onShutdownfailure(x: SupervisorMsg): Unit = {
    println(s"Supervisor in ShutdownFailure received an unexpected message: $x ")
  }

  private def onShutdown(x: SupervisorMsg): Unit = {
    println(s"Supervisor in Shutdown received an unexpected message: $x ")
  }

  private def onInitialized(componentRef: ActorRef[InitialMsg]): Unit = {
    registerWithLocationService()
    componentRef ! Run
  }

  private def onInitializeFailure(msg: String): Unit = {
    lifecycleState = LifecycleInitializeFailure
    mode = SupervisorMode.LifecycleFailure
  }

  private def onRunningComponent(componentRef: ActorRef[RunningMsg]): Unit = {
    runningComponent = componentRef
    lifecycleState = LifecycleRunning
    mode = SupervisorMode.Running
    notifyListeners(LifecycleStateChanged(LifecycleRunning))
  }

  def onShutdownTimeOut(): Unit = {
    lifecycleState = LifecycleShutdownFailure
    notifyListeners(LifecycleStateChanged(LifecycleShutdownFailure))
    if (haltingFlag) haltComponent()
    mode = SupervisorMode.ShutdownFailure
  }

  private def onShutdownFailure(reason: String): Unit = ???

  private def onShutdownComplete(): Unit = {
    shutdownTimer.map(_.cancel())
    lifecycleState = LifecycleShutdown
    notifyListeners(LifecycleStateChanged(LifecycleShutdown))
    if (haltingFlag) haltComponent()
    mode = SupervisorMode.Shutdown
  }

  private def scheduleTimeout = {
    ctx.schedule(shutdownTimeout, ctx.self, ShutdownTimeout)
  }

  private def onHalt(): Unit = {
    haltingFlag = true
    ctx.self ! ExComponentShutdown
  }

  private def addListener(l: ActorRef[LifecycleStateChanged]): Unit = listeners = listeners + l

  private def removeListener(l: ActorRef[LifecycleStateChanged]): Unit = listeners = listeners - l

  private def notifyListeners(msg: LifecycleStateChanged): Unit = {
    listeners.foreach(_ ! msg)
  }

  private def registerWithLocationService(): Unit = ???

  private def unregisterFromLocationService(): Unit = ???

  private def haltComponent(): Unit = ???
}
