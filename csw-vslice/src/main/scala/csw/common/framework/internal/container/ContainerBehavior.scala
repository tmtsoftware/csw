package csw.common.framework.internal.container

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{Behavior, PostStop, Signal, Terminated}
import csw.common.framework.internal.supervisor.{SupervisorInfoFactory, SupervisorMode}
import csw.common.framework.models.ContainerCommonMessage.{GetComponents, GetContainerMode}
import csw.common.framework.models.ContainerIdleMessage.{RegistrationComplete, RegistrationFailed, SupervisorsCreated}
import csw.common.framework.models.FromSupervisorMessage.SupervisorModeChanged
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models._
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models._
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

import scala.concurrent.{ExecutionContext, Future}
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
  var supervisors: Set[SupervisorInfo]            = Set.empty
  var runningComponents: Set[SupervisorInfo]      = Set.empty
  var mode: ContainerMode                         = ContainerMode.Idle
  var registrationOpt: Option[RegistrationResult] = None

  registerWithLocationService()
  createComponents(containerInfo.components)

  override def onMessage(msg: ContainerMessage): Behavior[ContainerMessage] = {
    (mode, msg) match {
      case (_, msg: ContainerCommonMessage)                ⇒ onCommon(msg)
      case (ContainerMode.Idle, msg: ContainerIdleMessage) ⇒ onIdle(msg)
      case (ContainerMode.Running, msg: Lifecycle)         ⇒ supervisors.foreach(_.component.supervisor ! msg)
      case (_, message)                                    ⇒ println(s"Container in $mode received an unexpected message: $message")
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
    case GetComponents(replyTo) ⇒
      replyTo ! Components(supervisors.map(_.component))
    case GetContainerMode(replyTo) ⇒
      replyTo ! mode
    case Restart ⇒
      mode = ContainerMode.Idle
      runningComponents = Set.empty
      supervisors.foreach(_.component.supervisor ! Restart)
    case Shutdown ⇒
      mode = ContainerMode.Idle
      supervisors.foreach(_.component.supervisor ! Shutdown)
  }

  def onIdle(idleContainerMessage: ContainerIdleMessage): Unit = idleContainerMessage match {
    case SupervisorsCreated(supervisorInfos) ⇒
      supervisors = supervisorInfos
      supervisors.foreach(supervisor ⇒ ctx.watch(supervisor.component.supervisor))
      updateRunningComponents()
    case SupervisorModeChanged(supervisor, supervisorMode) ⇒
      if (supervisorMode == SupervisorMode.Running) {
        runningComponents = (supervisors.find(_.component.supervisor == supervisor) ++ runningComponents).toSet
        updateRunningComponents()
      }
    case RegistrationComplete(registrationResult) ⇒
      registrationOpt = Some(registrationResult)
    case RegistrationFailed(throwable) ⇒
      println(s"log.error($throwable)") //FIXME use log statement
  }

  private def createComponents(componentInfos: Set[ComponentInfo]): Unit = {
    Future
      .traverse(componentInfos) { ci ⇒
        supervisorInfoFactory.make(ctx.self, ci, locationService)
      }
      .foreach(x ⇒ ctx.self ! SupervisorsCreated(x.flatten))
  }

  private def updateRunningComponents(): Unit = {
    if (runningComponents.size == supervisors.size) mode = ContainerMode.Running
  }

  private def registerWithLocationService(): Unit = {
    locationService.register(akkaRegistration).onComplete {
      case Success(registrationResult) ⇒ ctx.self ! RegistrationComplete(registrationResult)
      case Failure(throwable)          ⇒ ctx.self ! RegistrationFailed(throwable)
    }
  }
}
