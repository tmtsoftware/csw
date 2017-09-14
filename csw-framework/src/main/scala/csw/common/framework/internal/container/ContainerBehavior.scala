package csw.common.framework.internal.container

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
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ContainerBehavior(
    ctx: ActorContext[ContainerMessage],
    containerInfo: ContainerInfo,
    supervisorInfoFactory: SupervisorInfoFactory,
    registrationFactory: RegistrationFactory,
    locationService: LocationService
) extends ComponentLogger.TypedActor[ContainerMessage](ctx, containerInfo.name) {

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
    log.debug(s"Container in $mode state received a $msg message")
    (mode, msg) match {
      case (_, msg: ContainerCommonMessage)                ⇒ onCommon(msg)
      case (ContainerMode.Idle, msg: ContainerIdleMessage) ⇒ onIdle(msg)
      case (ContainerMode.Running, msg: Lifecycle)         ⇒ supervisors.foreach(_.component.supervisor ! msg)
      case (_, message) ⇒
        log.error(s"Container in $mode state received an unexpected $message message")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[ContainerMessage]] = {
    case Terminated(supervisor) ⇒
      log.error(s"Container in $mode state received terminated signal from [$supervisor] supervisor")
      supervisors = supervisors.filterNot(_.component.supervisor == supervisor.upcast)
      if (supervisors.isEmpty) ctx.system.terminate()
      this
    case PostStop ⇒
      log.error(s"Container is shutting down")
      registrationOpt.foreach(_.unregister())
      Behavior.stopped
  }

  def onCommon(commonContainerMessage: ContainerCommonMessage): Unit = commonContainerMessage match {
    case GetComponents(replyTo) ⇒
      replyTo ! Components(supervisors.map(_.component))
    case GetContainerMode(replyTo) ⇒
      replyTo ! mode
    case Restart ⇒
      log.debug(s"Container is changing state from $mode to ${ContainerMode.Idle}")
      mode = ContainerMode.Idle
      runningComponents = Set.empty
      supervisors.foreach(_.component.supervisor ! Restart)
    case Shutdown ⇒
      log.debug(s"Container is changing state from $mode to ${ContainerMode.Idle}")
      mode = ContainerMode.Idle
      supervisors.foreach(_.component.supervisor ! Shutdown)
  }

  def onIdle(idleContainerMessage: ContainerIdleMessage): Unit = idleContainerMessage match {
    case SupervisorsCreated(supervisorInfos) ⇒
      if (supervisorInfos.isEmpty) {
        log.error("Could not spawn any supervisor")
        ctx.system.terminate()
      } else {
        supervisors = supervisorInfos
        log.info(s"Container created following supervisors [${supervisors.map(_.component.supervisor).mkString(",")}]")
        supervisors.foreach(supervisorInfo ⇒ ctx.watch(supervisorInfo.component.supervisor))
        updateRunningComponents()
      }
    case SupervisorModeChanged(supervisor, supervisorMode) ⇒
      log.debug(s"Container received acknowledgement from supervisor [$supervisor] for state $supervisorMode")
      if (supervisorMode == SupervisorMode.Running) {
        runningComponents = (supervisors.find(_.component.supervisor == supervisor) ++ runningComponents).toSet
        updateRunningComponents()
      }
    case RegistrationComplete(registrationResult) ⇒
      registrationOpt = Some(registrationResult)
      log.info("Container registered itself with location service")
    case RegistrationFailed(throwable) ⇒
      log.error(throwable.getMessage, ex = throwable)
  }

  private def createComponents(componentInfos: Set[ComponentInfo]): Unit = {
    log.debug(s"Container is creating following components [${componentInfos.map(_.name).mkString(",")}]")
    Future
      .traverse(componentInfos) { ci ⇒
        supervisorInfoFactory.make(ctx.self, ci, locationService)
      }
      .foreach(x ⇒ ctx.self ! SupervisorsCreated(x.flatten))
  }

  private def updateRunningComponents(): Unit = {
    if (runningComponents.size == supervisors.size) {
      log.debug(s"Container is changing state from $mode to ${ContainerMode.Running}")
      mode = ContainerMode.Running
    }
  }

  private def registerWithLocationService(): Unit = {
    log.debug("Container is registering with location service")
    locationService.register(akkaRegistration).onComplete {
      case Success(registrationResult) ⇒ ctx.self ! RegistrationComplete(registrationResult)
      case Failure(throwable)          ⇒ ctx.self ! RegistrationFailed(throwable)
    }
  }
}
