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
import csw.common.framework.models.Component.{AssemblyInfo, ComponentInfo, HcdInfo}
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.models.LifecycleState._
import csw.common.framework.models.PreparingToShutdownMsg.{ShutdownComplete, ShutdownFailure, ShutdownTimeout}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.SupervisorExternalMsg._
import csw.common.framework.models.SupervisorIdleMsg.{InitializeFailure, Initialized, Running}
import csw.common.framework.models._
import csw.common.framework.scaladsl.assembly.AssemblyHandlersFactory
import csw.common.framework.scaladsl.hcd.HcdHandlersFactory
import csw.common.framework.scaladsl.supervisor.SupervisorMode.{Idle, PreparingToShutdown}
import csw.services.location.models.ComponentId

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

object Supervisor {
  private def registerWithLocationService(): Unit = ???

  private def unregisterFromLocationService(): Unit = ???

  private def terminated(actorRef: ActorRef[Nothing]): Unit = ???

  private def haltComponent(): Unit = ???

  private var listeners: Set[ActorRef[LifecycleStateChanged]] = Set[ActorRef[LifecycleStateChanged]]()

  private def addListener(l: ActorRef[LifecycleStateChanged]): Unit = listeners = listeners + l

  private def removeListener(l: ActorRef[LifecycleStateChanged]): Unit = listeners = listeners - l

  private def notifyListeners(msg: LifecycleStateChanged): Unit = {
    listeners.foreach(_ ! msg)
  }
}

class Supervisor(ctx: ActorContext[SupervisorMsg], componentInfo: ComponentInfo)
    extends MutableBehavior[SupervisorMsg] {

  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val name                          = componentInfo.componentName
  private val componentId                   = ComponentId(name, componentInfo.componentType)

  private val shutdownTimeout: FiniteDuration    = 5.seconds
  private var shutdownTimer: Option[Cancellable] = None

  private var haltingFlag = false

  var lifecycleState: LifecycleState         = LifecycleWaitingForInitialized
  var runningComponent: ActorRef[RunningMsg] = _
  var mode: SupervisorMode                   = Idle
  var isOnline: Boolean                      = false

  val component: ActorRef[Nothing] = {
    val behavior = componentInfo match {
      case x: HcdInfo      ⇒ hcdBehavior(x, ctx.self.narrow[FromComponentLifecycleMessage])
      case x: AssemblyInfo ⇒ assemblyBehavior(x, ctx.self.narrow[FromComponentLifecycleMessage])
    }

    ctx.spawnAnonymous[Nothing](behavior)
  }

  ctx.watch(component)

  override def onMessage(msg: SupervisorMsg): Behavior[SupervisorMsg] = {
    (mode, msg) match {
      case (Idle, msg: SupervisorIdleMsg)                       => onIdleMessages(msg)
      case (Idle, msg: CommonSupervisorMsg)                     => onCommonMessages(msg)
      case (SupervisorMode.Running, msg: SupervisorExternalMsg) => onRunning(msg)
      case (SupervisorMode.Running, msg: CommonSupervisorMsg)   => onCommonMessages(msg)
      case (PreparingToShutdown, msg: PreparingToShutdownMsg)   => onPreparingToShutdown(msg)
    }
    this
  }

  def onIdleMessages(msg: SupervisorIdleMsg): Unit = msg match {
    case Initialized(componentRef) => onInitialized(componentRef)
    case InitializeFailure(reason) => onInitializeFailure(reason)
    case Running(componentRef)     => onRunningComponent(componentRef)
  }

  private def onInitialized(componentRef: ActorRef[InitialMsg]): Unit = {
    Supervisor.registerWithLocationService()
    componentRef ! Run
    lifecycleState = LifecycleRunning
    Supervisor.notifyListeners(LifecycleStateChanged(LifecycleRunning))
    mode = SupervisorMode.Running
  }

  private def onInitializeFailure(msg: String): Unit = {
    lifecycleState = LifecycleInitializeFailure
    mode = SupervisorMode.Failure
  }

  private def onRunningComponent(componentRef: ActorRef[RunningMsg]): Unit = {}

  def onCommonMessages(msg: CommonSupervisorMsg): Unit = msg match {
    case SubscribeLifecycleCallback(actorRef)   => Supervisor.addListener(actorRef)
    case UnsubscribeLifecycleCallback(actorRef) => Supervisor.removeListener(actorRef)
    case HaltComponent                          => onHalt()
  }

  def onRunning(msg: SupervisorExternalMsg): Unit = msg match {
    case LifecycleStateChanged(state) =>
    case ExComponentRestart           =>
    case ExComponentShutdown          => onShutDown()
    case ExComponentOnline            =>
    case ExComponentOffline           =>
  }

  def onPreparingToShutdown(msg: PreparingToShutdownMsg): Unit = msg match {
    case ShutdownTimeout         => onShutdownTimeOut()
    case ShutdownFailure(reason) => onShutdownFailure(reason)
    case ShutdownComplete        => onShutdownComplete()
  }

  def onShutdownTimeOut(): Unit = {
    lifecycleState = LifecycleShutdownFailure
    Supervisor.notifyListeners(LifecycleStateChanged(LifecycleShutdownFailure))
    if (haltingFlag) Supervisor.haltComponent()
    mode = SupervisorMode.ShutdownFailure
  }

  private def onShutdownFailure(reason: String): Unit = ???

  private def onShutdownComplete(): Unit = {
    shutdownTimer.map(_.cancel())
    lifecycleState = LifecycleShutdown
    Supervisor.notifyListeners(LifecycleStateChanged(LifecycleShutdown))
    if (haltingFlag) Supervisor.haltComponent()
    mode = SupervisorMode.Shutdown
  }

  private def onShutDown(): Unit = {
    shutdownTimer = Some(scheduleTimeout)
    runningComponent ! Lifecycle(ToComponentLifecycleMessage.Shutdown)
    Supervisor.unregisterFromLocationService()
    lifecycleState = LifecyclePreparingToShutdown
    mode = PreparingToShutdown
  }

  private def scheduleTimeout = {
    ctx.schedule(shutdownTimeout, ctx.self, ShutdownTimeout)
  }

  private def hcdBehavior(hcdInfo: HcdInfo, supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] = {
    val hcdClass   = Class.forName(hcdInfo.componentClassName + "Factory")
    val hcdFactory = hcdClass.newInstance().asInstanceOf[HcdHandlersFactory[_]]
    hcdFactory.behavior(hcdInfo, supervisor)
  }

  private def assemblyBehavior(assemblyInfo: AssemblyInfo,
                               supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] = {
    val assemblyClass   = Class.forName(assemblyInfo.componentClassName + "Factory")
    val assemblyFactory = assemblyClass.newInstance().asInstanceOf[AssemblyHandlersFactory[_]]
    assemblyFactory.behavior(assemblyInfo, supervisor)
  }

  private def onHalt(): Unit = {
    haltingFlag = true
    ctx.self ! ExComponentShutdown
  }
}
