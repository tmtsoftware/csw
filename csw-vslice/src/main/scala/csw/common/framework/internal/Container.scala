package csw.common.framework.internal

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.CommonSupervisorMsg.LifecycleStateSubscription
import csw.common.framework.models.ContainerMsg.{GetComponents, GetContainerMode}
import csw.common.framework.models.IdleContainerMsg.{RegistrationComplete, RegistrationFailed, SupervisorModeChanged}
import csw.common.framework.models.PubSub.{Subscribe, Unsubscribe}
import csw.common.framework.models.RunningContainerMsg.{UnRegistrationComplete, UnRegistrationFailed}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage._
import csw.common.framework.models._
import csw.common.framework.scaladsl.SupervisorFactory
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models._
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class Container(
    ctx: ActorContext[ContainerMsg],
    containerInfo: ContainerInfo,
    locationService: LocationService,
    supervisorFactory: SupervisorFactory,
    registrationFactory: RegistrationFactory
) extends MutableBehavior[ContainerMsg] {

  val componentId                                 = ComponentId(containerInfo.name, ComponentType.Container)
  var supervisors: List[SupervisorInfo]           = List.empty
  var runningComponents: List[SupervisorInfo]     = List.empty
  var mode: ContainerMode                         = ContainerMode.Idle
  var registrationOpt: Option[RegistrationResult] = None
  val akkaRegistration: AkkaRegistration          = registrationFactory.akkaTyped(AkkaConnection(componentId), ctx.self)
  val lifecycleStateTrackerRef: ActorRef[LifecycleStateChanged] =
    ctx.spawnAdapter(SupervisorModeChanged, "LifecycleChangeAdapter")

  createComponents(containerInfo.components)

  override def onMessage(msg: ContainerMsg): Behavior[ContainerMsg] = {
    (mode, msg) match {
      case (_, GetComponents(replyTo))                              ⇒ replyTo ! Components(supervisors)
      case (_, GetContainerMode(replyTo))                           ⇒ replyTo ! mode
      case (ContainerMode.Running, Lifecycle(Restart))              ⇒ onRestart()
      case (ContainerMode.Running, Lifecycle(Shutdown))             ⇒ onShutdown()
      case (ContainerMode.Running, Lifecycle(lifecycleMsg))         ⇒ sendLifecycleMsgToAllComponents(lifecycleMsg)
      case (ContainerMode.Running, UnRegistrationComplete)          ⇒ onUnRegistrationComplete()
      case (ContainerMode.Running, UnRegistrationFailed(throwable)) ⇒ onUnRegistrationFailed(throwable)
      case (ContainerMode.Idle, SupervisorModeChanged(lifecycleStateChanged)) ⇒
        onLifecycleStateChanged(lifecycleStateChanged)
      case (ContainerMode.Idle, RegistrationComplete(registrationResult)) ⇒ onRegistrationComplete(registrationResult)
      case (ContainerMode.Idle, RegistrationFailed(throwable))            ⇒ onRegistrationFailure(throwable)
      case (containerMode, message)                                       ⇒ println(s"Container in $containerMode received an unexpected message: $message")
    }
    this
  }

  def onRestart(): Unit = {
    mode = ContainerMode.Idle
    subscribeLifecycleTrackerWithAllSupervisors()
    sendLifecycleMsgToAllComponents(Restart)
  }

  def onShutdown(): Unit = unregisterFromLocationService()

  def sendLifecycleMsgToAllComponents(lifecycleMsg: ToComponentLifecycleMessage): Unit = {
    supervisors.foreach { _.supervisor ! Lifecycle(lifecycleMsg) }
  }

  def onLifecycleStateChanged(lifecycleStateChanged: LifecycleStateChanged): Unit = {
    if (lifecycleStateChanged.state == SupervisorMode.Running) {
      val componentSupervisor = lifecycleStateChanged.publisher
      unsubscribeLifecycleTracker(componentSupervisor)
      updateRunningComponents(componentSupervisor)
    }
  }

  private def createComponents(componentInfos: Set[ComponentInfo]): Unit = {
    supervisors = supervisors ::: componentInfos.flatMap(createComponent).toList
    subscribeLifecycleTrackerWithAllSupervisors()
  }

  private def createComponent(componentInfo: ComponentInfo): Option[SupervisorInfo] = {
    supervisors.find(_.componentInfo == componentInfo) match {
      case Some(_) => None
      case None    => Some(supervisorFactory.make(componentInfo))
    }
  }

  private def subscribeLifecycleTrackerWithAllSupervisors(): Unit = {
    supervisors.foreach(_.supervisor ! LifecycleStateSubscription(Subscribe(lifecycleStateTrackerRef)))
  }

  private def unsubscribeLifecycleTracker(componentSupervisor: ActorRef[SupervisorExternalMessage]): Unit = {
    componentSupervisor ! LifecycleStateSubscription(Unsubscribe(lifecycleStateTrackerRef))
  }

  private def updateRunningComponents(componentSupervisor: ActorRef[SupervisorExternalMessage]): Unit = {
    runningComponents = (supervisors.find(_.supervisor == componentSupervisor) ++ runningComponents).toList
    if (runningComponents.size == supervisors.size)
      registerWithLocationService()
  }

  private def registerWithLocationService(): Unit = {
    val registrationResultF = locationService.register(akkaRegistration)
    pipeRegistrationToSelf(registrationResultF)
  }

  private def pipeRegistrationToSelf(registrationResultF: Future[RegistrationResult]): Unit = {
    registrationResultF.onComplete {
      case Success(registrationResult) ⇒ ctx.self ! RegistrationComplete(registrationResult)
      case Failure(throwable)          ⇒ ctx.self ! RegistrationFailed(throwable)
    }
  }

  private def onRegistrationComplete(registrationResult: RegistrationResult): Unit = {
    registrationOpt = Some(registrationResult)
    mode = ContainerMode.Running
    runningComponents = List.empty
  }

  private def onRegistrationFailure(throwable: Throwable): Unit =
    println(s"log.error($throwable)") //FIXME use log statement

  private def unregisterFromLocationService(): Any = {
    registrationOpt match {
      case Some(registrationResult) ⇒
        pipeUnregisterationToSelf(registrationResult.unregister())
      case None ⇒
        println("log.warn(No valid RegistrationResult found to unregister.)") //FIXME to log error
    }
  }

  private def pipeUnregisterationToSelf(registrationResultF: Future[_]): Unit = {
    registrationResultF.onComplete {
      case Success(_)         ⇒ ctx.self ! UnRegistrationComplete
      case Failure(throwable) ⇒ ctx.self ! UnRegistrationFailed(throwable)
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
