package csw.common.framework.internal.container

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.internal.supervisor.{SupervisorInfoFactory, SupervisorMode}
import csw.common.framework.models.ContainerCommonMessage.GetContainerMode
import csw.common.framework.models.ContainerExternalMessage.GetComponents
import csw.common.framework.models.ContainerIdleMessage.{
  RegistrationComplete,
  RegistrationFailed,
  SupervisorModeChanged
}
import csw.common.framework.models.ContainerRunningMessage.{UnRegistrationComplete, UnRegistrationFailed}
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage._
import csw.common.framework.models._
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models._
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ContainerBehavior(
    ctx: ActorContext[ContainerMessage],
    containerInfo: ContainerInfo,
    supervisorInfoFactory: SupervisorInfoFactory,
    registrationFactory: RegistrationFactory,
    locationService: LocationService
) extends MutableBehavior[ContainerMessage] {

  implicit val ec: ExecutionContext = ctx.executionContext

  val componentId                                 = ComponentId(containerInfo.name, ComponentType.Container)
  val akkaRegistration: AkkaRegistration          = registrationFactory.akkaTyped(AkkaConnection(componentId), ctx.self)
  var supervisors: List[SupervisorInfo]           = List.empty
  var runningComponents: Set[SupervisorInfo]      = Set.empty
  var mode: ContainerMode                         = ContainerMode.Idle
  var registrationOpt: Option[RegistrationResult] = None

  createComponents(containerInfo.components)

  override def onMessage(msg: ContainerMessage): Behavior[ContainerMessage] = {
    (mode, msg) match {
      case (_, msg: ContainerCommonMessage)                      ⇒ onCommon(msg)
      case (ContainerMode.Idle, msg: ContainerIdleMessage)       ⇒ onIdle(msg)
      case (ContainerMode.Running, msg: ContainerRunningMessage) ⇒ onRunning(msg)
      case (_, message)                                          ⇒ println(s"Container in $mode received an unexpected message: $message")
    }
    this
  }

  def onCommon(commonContainerMessage: ContainerCommonMessage): Unit = commonContainerMessage match {
    case GetComponents(replyTo)    ⇒ replyTo ! Components(supervisors)
    case GetContainerMode(replyTo) ⇒ replyTo ! mode
  }

  def onIdle(idleContainerMessage: ContainerIdleMessage): Unit = idleContainerMessage match {
    case SupervisorModeChanged(supervisor, supervisorMode) ⇒ onSupervisorModeChange(supervisor, supervisorMode)
    case RegistrationComplete(registrationResult)          ⇒ onRegistrationComplete(registrationResult)
    case RegistrationFailed(throwable)                     ⇒ onRegistrationFailure(throwable)
  }

  def onRunning(runningContainerMessage: ContainerRunningMessage): Unit = runningContainerMessage match {
    case Lifecycle(Restart)              ⇒ onRestart()
    case Lifecycle(Shutdown)             ⇒ onShutdown()
    case Lifecycle(lifecycleMessage)     ⇒ sendLifecycleMessageToAllComponents(lifecycleMessage)
    case UnRegistrationComplete          ⇒ onUnRegistrationComplete()
    case UnRegistrationFailed(throwable) ⇒ onUnRegistrationFailed(throwable)
  }

  def onRestart(): Unit = {
    mode = ContainerMode.Idle
    sendLifecycleMessageToAllComponents(Restart)
  }

  def onShutdown(): Unit = unregisterFromLocationService()

  def sendLifecycleMessageToAllComponents(lifecycleMessage: ToComponentLifecycleMessage): Unit = {
    supervisors.foreach { _.supervisor ! Lifecycle(lifecycleMessage) }
  }

  private def createComponents(componentInfos: Set[ComponentInfo]): Unit =
    supervisors = componentInfos.flatMap(createComponent).toList

  private def createComponent(componentInfo: ComponentInfo): Option[SupervisorInfo] = {
    if (supervisors.exists(_.componentInfo == componentInfo)) None
    else Some(supervisorInfoFactory.make(ctx.self, componentInfo))
  }

  private def onSupervisorModeChange(supervisor: ActorRef[SupervisorExternalMessage],
                                     supervisorMode: SupervisorMode): Unit = {
    if (supervisorMode == SupervisorMode.Running) {
      updateRunningComponents(supervisor)
    }
  }

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
    sendLifecycleMessageToAllComponents(Shutdown)
  }

  private def onUnRegistrationFailed(throwable: Throwable): Unit =
    println(s"log.error() with $throwable") //FIXME use log statement
}
