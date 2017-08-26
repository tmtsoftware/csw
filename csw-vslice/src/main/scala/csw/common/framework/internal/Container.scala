package csw.common.framework.internal

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.ContainerCommonMsg.{GetComponents, GetContainerMode}
import csw.common.framework.models.SupervisorCommonMsg.{GetSupervisorMode, LifecycleStateSubscription}
import csw.common.framework.models.ContainerIdleMsg.{RegistrationComplete, RegistrationFailed, SupervisorModeChanged}
import csw.common.framework.models.PubSub.{Subscribe, Unsubscribe}
import csw.common.framework.models.ContainerRunningMsg.{UnRegistrationComplete, UnRegistrationFailed}
import csw.common.framework.models.ModeMsg.{ContainerModeMsg, SupervisorModeMsg}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage._
import csw.common.framework.models._
import csw.common.framework.scaladsl.SupervisorFactory
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models._
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class Container(
    ctx: ActorContext[ContainerMsg],
    containerInfo: ContainerInfo,
    supervisorFactory: SupervisorFactory,
    registrationFactory: RegistrationFactory,
    locationService: LocationService
) extends MutableBehavior[ContainerMsg] {

  implicit val ec: ExecutionContext = ctx.executionContext

  val componentId                                 = ComponentId(containerInfo.name, ComponentType.Container)
  val akkaRegistration: AkkaRegistration          = registrationFactory.akkaTyped(AkkaConnection(componentId), ctx.self)
  var supervisors: List[SupervisorInfo]           = List.empty
  var runningComponents: Set[SupervisorInfo]      = Set.empty
  var mode: ContainerMode                         = ContainerMode.Idle
  var registrationOpt: Option[RegistrationResult] = None
  val lifecycleStateTrackerRef: ActorRef[LifecycleStateChanged] =
    ctx.spawnAdapter(SupervisorModeChanged, "LifecycleStateTracker")

  createComponents(containerInfo.components)

  override def onMessage(msg: ContainerMsg): Behavior[ContainerMsg] = {
    (mode, msg) match {
      case (_, msg: ContainerCommonMsg)                      ⇒ onCommon(msg)
      case (ContainerMode.Idle, msg: ContainerIdleMsg)       ⇒ onIdle(msg)
      case (ContainerMode.Running, msg: ContainerRunningMsg) ⇒ onRunning(msg)
      case (_, message)                                      ⇒ println(s"Container in $mode received an unexpected message: $message")
    }
    this
  }

  def onCommon(commonContainerMsg: ContainerCommonMsg): Unit = commonContainerMsg match {
    case GetComponents(replyTo)    ⇒ replyTo ! Components(supervisors)
    case GetContainerMode(replyTo) ⇒ replyTo ! ContainerModeMsg(mode)
  }

  def onIdle(idleContainerMsg: ContainerIdleMsg): Unit = idleContainerMsg match {
    case SupervisorModeChanged(LifecycleStateChanged(publisher, lifecycleState)) ⇒
      onSupervisorModeChange(publisher, lifecycleState)
    case SupervisorModeMsg(supervisor, supervisorMode) ⇒ onSupervisorModeChange(supervisor, supervisorMode)
    case RegistrationComplete(registrationResult)      ⇒ onRegistrationComplete(registrationResult)
    case RegistrationFailed(throwable)                 ⇒ onRegistrationFailure(throwable)
  }

  def onRunning(runningContainerMsg: ContainerRunningMsg): Unit = runningContainerMsg match {
    case Lifecycle(Restart)              ⇒ onRestart()
    case Lifecycle(Shutdown)             ⇒ onShutdown()
    case Lifecycle(lifecycleMsg)         ⇒ sendLifecycleMsgToAllComponents(lifecycleMsg)
    case UnRegistrationComplete          ⇒ onUnRegistrationComplete()
    case UnRegistrationFailed(throwable) ⇒ onUnRegistrationFailed(throwable)
  }

  def onRestart(): Unit = {
    mode = ContainerMode.Idle
    subscribeToSupervisorsLifecycle()
    sendLifecycleMsgToAllComponents(Restart)
  }

  def onShutdown(): Unit = unregisterFromLocationService()

  def sendLifecycleMsgToAllComponents(lifecycleMsg: ToComponentLifecycleMessage): Unit = {
    supervisors.foreach { _.supervisor ! Lifecycle(lifecycleMsg) }
  }

  private def createComponents(componentInfos: Set[ComponentInfo]): Unit = {
    supervisors = supervisors ::: componentInfos.flatMap(createComponent).toList
    subscribeToSupervisorsLifecycle()
    requestSupervisorMode() // Get all supervisors' mode so that if any supervisor got changed to 'running' mode before this container could subscribe to it's lifecycle changes then it gets handled

  }

  private def createComponent(componentInfo: ComponentInfo): Option[SupervisorInfo] = {
    if (supervisors.exists(_.componentInfo == componentInfo)) None
    else Some(supervisorFactory.make(componentInfo))
  }

  private def subscribeToSupervisorsLifecycle(): Unit =
    supervisors.foreach(_.supervisor ! LifecycleStateSubscription(Subscribe(lifecycleStateTrackerRef)))

  private def requestSupervisorMode(): Unit =
    supervisors.foreach(_.supervisor ! GetSupervisorMode(ctx.self))

  private def onSupervisorModeChange(supervisor: ActorRef[SupervisorExternalMessage],
                                     supervisorMode: SupervisorMode): Unit = {
    if (supervisorMode == SupervisorMode.Running) {
      unSubscribeFromSupervisorsLifecycle(supervisor)
      updateRunningComponents(supervisor)
    }
  }

  private def unSubscribeFromSupervisorsLifecycle(componentSupervisor: ActorRef[SupervisorExternalMessage]): Unit =
    componentSupervisor ! LifecycleStateSubscription(Unsubscribe(lifecycleStateTrackerRef))

  private def updateRunningComponents(componentSupervisor: ActorRef[SupervisorExternalMessage]): Unit = {
    runningComponents = (supervisors.find(_.supervisor == componentSupervisor) ++ runningComponents).toSet
    if (runningComponents.size == supervisors.size)
      registerWithLocationService()
  }

  private def registerWithLocationService(): Unit = {
    locationService.register(akkaRegistration).onComplete {
      case Success(registrationResult) ⇒ ctx.self ! RegistrationComplete(registrationResult)
      case Failure(throwable)          ⇒ ctx.self ! RegistrationFailed(throwable)
    }
  }

  private def onRegistrationComplete(registrationResult: RegistrationResult): Unit = {
    registrationOpt = Some(registrationResult)
    mode = ContainerMode.Running
    runningComponents = Set.empty
  }

  private def onRegistrationFailure(throwable: Throwable): Unit =
    println(s"log.error($throwable)") //FIXME use log statement

  private def unregisterFromLocationService(): Any = {
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

  private def onUnRegistrationComplete(): Unit = {
    mode = ContainerMode.Idle
    registrationOpt = None
    sendLifecycleMsgToAllComponents(Shutdown)
  }

  private def onUnRegistrationFailed(throwable: Throwable): Unit =
    println(s"log.error() with $throwable") //FIXME use log statement
}
