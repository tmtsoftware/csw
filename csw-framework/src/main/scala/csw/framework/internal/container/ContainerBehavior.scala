package csw.framework.internal.container

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.{ActorRef, Behavior, PostStop, Signal, Terminated}
import csw.framework.internal.supervisor.{SupervisorInfoFactory, SupervisorLifecycleState}
import csw.framework.models.ContainerCommonMessage.{GetComponents, GetContainerLifecycleState, RegistrationComplete, RegistrationFailed}
import csw.framework.models.ContainerIdleMessage.SupervisorsCreated
import csw.framework.models.FromSupervisorMessage.SupervisorLifecycleStateChanged
import csw.framework.models.RunningMessage.Lifecycle
import csw.framework.models._
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
) extends ComponentLogger.TypedActor[ContainerMessage](ctx, Some(containerInfo.name)) {

  implicit val ec: ExecutionContext = ctx.executionContext

  val componentId                                 = ComponentId(containerInfo.name, ComponentType.Container)
  val akkaRegistration: AkkaRegistration          = registrationFactory.akkaTyped(AkkaConnection(componentId), ctx.self)
  var supervisors: Set[SupervisorInfo]            = Set.empty
  var runningComponents: Set[ActorRef[SupervisorExternalMessage]]      = Set.empty
  var lifecycleState: ContainerLifecycleState     = ContainerLifecycleState.Idle
  var registrationOpt: Option[RegistrationResult] = None

  registerWithLocationService()
  createComponents(containerInfo.components)

  override def onMessage(msg: ContainerMessage): Behavior[ContainerMessage] = {
    log.debug(s"Container in lifecycle state :[$lifecycleState] received message :[$msg]")
    (lifecycleState, msg) match {
      case (_, msg: ContainerCommonMessage)                          ⇒ onCommon(msg)
      case (ContainerLifecycleState.Idle, msg: ContainerIdleMessage) ⇒ onIdle(msg)
      case (ContainerLifecycleState.Running, msg: Lifecycle)         ⇒ supervisors.foreach(_.component.supervisor ! msg)
      case (_, message) ⇒
        log.error(s"Unexpected message :[$message] received by container in lifecycle state :[$lifecycleState]")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[ContainerMessage]] = {
    case Terminated(supervisor) ⇒
      log.warn(
        s"Container in lifecycle state :[$lifecycleState] received terminated signal from supervisor :[$supervisor]"
      )
      supervisors = supervisors.filterNot(_.component.supervisor == supervisor.upcast)
      if (supervisors.isEmpty) {
        log.warn("All supervisors from this container are terminated. Initiating co-ordinated shutdown.")
        coordinatedShutdown()
      }
      this
    case PostStop ⇒
      log.warn(s"Un-registering container from location service")
      registrationOpt.foreach(_.unregister())
      Behavior.stopped
  }

  def onCommon(commonContainerMessage: ContainerCommonMessage): Unit = commonContainerMessage match {
    case RegistrationComplete(registrationResult) ⇒
      registrationOpt = Some(registrationResult)
    case RegistrationFailed(throwable) ⇒
      log.error(throwable.getMessage, ex = throwable)
    case GetComponents(replyTo) ⇒
      replyTo ! Components(supervisors.map(_.component))
    case GetContainerLifecycleState(replyTo) ⇒
      replyTo ! lifecycleState
    case Restart ⇒
      log.debug(s"Container is changing lifecycle state from [$lifecycleState] to [${ContainerLifecycleState.Idle}]")
      lifecycleState = ContainerLifecycleState.Idle
      runningComponents = Set.empty
      supervisors.foreach(_.component.supervisor ! Restart)
    case Shutdown ⇒
      log.debug(s"Container is changing lifecycle state from [$lifecycleState] to [${ContainerLifecycleState.Idle}]")
      lifecycleState = ContainerLifecycleState.Idle
      supervisors.foreach(_.component.supervisor ! Shutdown)
  }

  def onIdle(idleContainerMessage: ContainerIdleMessage): Unit = idleContainerMessage match {
    case SupervisorsCreated(supervisorInfos) ⇒
      if (supervisorInfos.isEmpty) {
        log.error(s"Failed to spawn supervisors for ComponentInfo's :[${containerInfo.components.mkString(", ")}]")
        coordinatedShutdown()
      } else {
        supervisors = supervisorInfos
        log.info(s"Container created following supervisors :[${supervisors.map(_.component.supervisor).mkString(",")}]")
        supervisors.foreach(supervisorInfo ⇒ ctx.watch(supervisorInfo.component.supervisor))
        updateContainerStateToRunning()
      }
    case SupervisorLifecycleStateChanged(supervisor, SupervisorLifecycleState.Running) ⇒
        runningComponents = runningComponents + supervisor
        updateContainerStateToRunning()
  }

  private def createComponents(componentInfos: Set[ComponentInfo]): Unit = {
    log.info(s"Container is creating following components :[${componentInfos.map(_.name).mkString(", ")}]")
    Future
      .traverse(componentInfos) { ci ⇒
        supervisorInfoFactory.make(ctx.self, ci, locationService)
      }
      .foreach(x ⇒ ctx.self ! SupervisorsCreated(x.flatten))
  }

  private def updateContainerStateToRunning(): Unit = {
    if (runningComponents.size == supervisors.size) {
      log.debug(s"Container is changing lifecycle state from [$lifecycleState] to [${ContainerLifecycleState.Running}]")
      lifecycleState = ContainerLifecycleState.Running
    }
  }

  private def registerWithLocationService(): Unit = {
    log.debug(
      s"Container with connection :[${akkaRegistration.connection.name}] is registering with location service with ref :[${akkaRegistration.actorRef}]"
    )
    locationService.register(akkaRegistration).onComplete {
      case Success(registrationResult) ⇒ ctx.self ! RegistrationComplete(registrationResult)
      case Failure(throwable)          ⇒ ctx.self ! RegistrationFailed(throwable)
    }
  }

  private def coordinatedShutdown(): Future[Done] = CoordinatedShutdown(ctx.system.toUntyped).run()
}
