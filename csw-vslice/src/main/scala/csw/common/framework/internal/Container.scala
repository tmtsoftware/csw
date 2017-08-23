package csw.common.framework.internal

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior, PostStop, Signal}
import csw.common.framework.models.CommonSupervisorMsg.LifecycleStateSubscription
import csw.common.framework.models.ContainerMsg.GetComponents
import csw.common.framework.models.IdleContainerMsg.{RegistrationComplete, SupervisorModeChanged}
import csw.common.framework.models.PubSub.{Subscribe, Unsubscribe}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage._
import csw.common.framework.models._
import csw.common.framework.scaladsl.SupervisorFactory
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import scala.util.{Failure, Success}

class Container(
    ctx: ActorContext[ContainerMsg],
    containerInfo: ContainerInfo,
    supervisorFactory: SupervisorFactory,
    locationService: LocationService
) extends MutableBehavior[ContainerMsg] {

  val componentId                                 = ComponentId(containerInfo.name, ComponentType.Container)
  var supervisors: List[SupervisorInfo]           = List.empty
  var runningComponents: List[SupervisorInfo]     = List.empty
  var mode: ContainerMode                         = ContainerMode.Idle
  var registrationOpt: Option[RegistrationResult] = None
  val lifecycleStateTrackerRef: ActorRef[LifecycleStateChanged] =
    ctx.spawnAdapter(SupervisorModeChanged, "LifecycleChangeAdapter")

  registerWithLocationService()

  createComponents(containerInfo.components)

  override def onMessage(msg: ContainerMsg): Behavior[ContainerMsg] = {
    (mode, msg) match {
      case (_, GetComponents(replyTo))                      ⇒ replyTo ! Components(supervisors)
      case (ContainerMode.Running, Lifecycle(Restart))      ⇒ onRestart()
      case (ContainerMode.Running, Lifecycle(Shutdown))     ⇒ onShutdown()
      case (ContainerMode.Running, Lifecycle(lifecycleMsg)) ⇒ sendLifecycleMsgToAllComponents(lifecycleMsg)
      case (ContainerMode.Idle, SupervisorModeChanged(lifecycleStateChanged)) ⇒
        onLifecycleStateChanged(lifecycleStateChanged)
      case (containerMode, message) ⇒ println(s"Container in $containerMode received an unexpected message: $message")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[ContainerMsg]] = {
    case PostStop ⇒
      unregisterFromLocationService()
      this
  }

  def onRestart(): Unit = {
    mode = ContainerMode.Idle
    subscribeLifecycleTrackerWithAllSupervisors()
    sendLifecycleMsgToAllComponents(Restart)
  }

  def onShutdown(): Unit = {
    mode = ContainerMode.Idle
    unregisterFromLocationService()
    sendLifecycleMsgToAllComponents(Shutdown)
  }

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
      case None    => Some(SupervisorInfo(supervisorFactory.make(componentInfo), componentInfo))
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
    if (runningComponents.size == supervisors.size) {
      registerWithLocationService()
    }
  }

  private def registerWithLocationService(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val registrationResultF = locationService.register(Registration.akkaTyped(AkkaConnection(componentId), ctx.self))
    registrationResultF.onComplete {
      case Success(registrationResult) ⇒ ctx.self ! RegistrationComplete(registrationResult)
      case Failure(ex)                 ⇒ println("log.error()")
    }
  }

  private def onRegistrationComplete(registrationResult: RegistrationResult): Unit = {
    registrationOpt = Some(registrationResult)
    mode = ContainerMode.Running
    runningComponents = List.empty
  }

  private def unregisterFromLocationService(): Any = {
    registrationOpt match {
      case Some(registrationResult) ⇒
        Await.result(registrationResult.unregister(), 10.seconds)
      //TODO: change this to logging
      case None ⇒ println("No valid RegistrationResult found; can't unregister.")
    }
  }
}
