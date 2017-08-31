package csw.common.framework.internal.container

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior, PostStop, Signal, Terminated}
import csw.common.framework.internal.supervisor.{SupervisorInfoFactory, SupervisorMode}
import csw.common.framework.models.ContainerCommonMessage.{GetComponents, GetContainerMode}
import csw.common.framework.models.ContainerIdleMessage.{RegistrationComplete, RegistrationFailed}
import csw.common.framework.models.FromSupervisorMessage.SupervisorModeChanged
import csw.common.framework.models.RunningMessage.Lifecycle
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
  supervisors.foreach(supervisor ⇒ ctx.watch(supervisor.component.supervisor))

  override def onMessage(msg: ContainerMessage): Behavior[ContainerMessage] = {
    (mode, msg) match {
      case (_, msg: ContainerCommonMessage)                      ⇒ onCommon(msg)
      case (ContainerMode.Idle, msg: ContainerIdleMessage)       ⇒ onIdle(msg)
      case (ContainerMode.Running, msg: ContainerRunningMessage) ⇒ onRunning(msg)
      case (_, message)                                          ⇒ println(s"Container in $mode received an unexpected message: $message")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[ContainerMessage]] = {
    case Terminated(supervisor) ⇒
      supervisors = supervisors.filterNot(_.component.supervisor == supervisor.upcast)
      if (supervisors.isEmpty) ctx.system.terminate()
      this
    case PostStop ⇒
      registrationOpt.foreach(_.unregister())
      Behavior.stopped
  }

  def onCommon(commonContainerMessage: ContainerCommonMessage): Unit = commonContainerMessage match {
    case GetComponents(replyTo)    ⇒ replyTo ! Components(supervisors.map(_.component))
    case Shutdown                  ⇒ supervisors.foreach(_.system.terminate())
    case GetContainerMode(replyTo) ⇒ replyTo ! mode

  }

  def onIdle(idleContainerMessage: ContainerIdleMessage): Unit = idleContainerMessage match {
    case SupervisorModeChanged(supervisor, supervisorMode) ⇒ onSupervisorModeChange(supervisor, supervisorMode)
    case RegistrationComplete(registrationResult)          ⇒ onRegistrationComplete(registrationResult)
    case RegistrationFailed(throwable)                     ⇒ onRegistrationFailure(throwable)
  }

  def onRunning(runningContainerMessage: ContainerRunningMessage): Unit = runningContainerMessage match {
    case Restart ⇒
      mode = ContainerMode.Idle
      supervisors.foreach { _.component.supervisor ! Restart }
    case Lifecycle(lifecycleMessage) ⇒
      sendLifecycleMessageToAllComponents(lifecycleMessage)
  }

  def sendLifecycleMessageToAllComponents(lifecycleMessage: ToComponentLifecycleMessage): Unit = {
    supervisors.foreach { _.component.supervisor ! Lifecycle(lifecycleMessage) }
  }

  private def createComponents(componentInfos: Set[ComponentInfo]): Unit =
    supervisors = componentInfos.flatMap(createComponent).toList

  private def createComponent(componentInfo: ComponentInfo): Option[SupervisorInfo] = {
    if (supervisors.exists(_.component.info == componentInfo)) None
    else Some(supervisorInfoFactory.make(ctx.self, componentInfo, locationService))
  }

  private def onSupervisorModeChange(
      supervisor: ActorRef[SupervisorExternalMessage],
      supervisorMode: SupervisorMode
  ): Unit = {
    if (supervisorMode == SupervisorMode.Running) {
      updateRunningComponents(supervisor)
    }
  }

  private def updateRunningComponents(componentSupervisor: ActorRef[SupervisorExternalMessage]): Unit = {
    runningComponents = (supervisors.find(_.component.supervisor == componentSupervisor) ++ runningComponents).toSet
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

}
