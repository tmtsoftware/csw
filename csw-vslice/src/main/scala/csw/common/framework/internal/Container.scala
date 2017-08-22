package csw.common.framework.internal

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior, PostStop, Signal}
import csw.common.framework.models.CommonSupervisorMsg.LifecycleStateSubscription
import csw.common.framework.models.ContainerMsg.{GetComponents, SupervisorModeChanged}
import csw.common.framework.models.PubSub.{Subscribe, Unsubscribe}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage._
import csw.common.framework.models._
import csw.common.framework.scaladsl.SupervisorBehaviorFactory
import csw.services.location.models.{ComponentId, ComponentType, RegistrationResult}

object Container {
  val lifecycleChangeAdapter = "LifecycleChangeAdapter"
}

class Container(ctx: ActorContext[ContainerMsg], containerInfo: ContainerInfo) extends MutableBehavior[ContainerMsg] {
  val componentId                                 = ComponentId(containerInfo.name, ComponentType.Container)
  var supervisors: List[SupervisorInfo]           = List.empty
  var runningComponents: List[SupervisorInfo]     = List.empty
  var mode: ContainerMode                         = ContainerMode.Idle
  var registrationOpt: Option[RegistrationResult] = None
  val lifecycleChangeAdapterActor: ActorRef[LifecycleStateChanged] =
    ctx.spawnAdapter(SupervisorModeChanged, "LifecycleChangeAdapter")

  registerWithLocationService()

  createComponents(containerInfo.components)

  override def onMessage(msg: ContainerMsg): Behavior[ContainerMsg] = {
    (mode, msg) match {
      case (_, GetComponents(replyTo))                      ⇒ replyTo ! Components(supervisors)
      case (ContainerMode.Running, Lifecycle(Restart))      ⇒ onRestart()
      case (ContainerMode.Running, Lifecycle(Shutdown))     ⇒ onShutdown()
      case (ContainerMode.Running, Lifecycle(lifecycleMsg)) ⇒ sendAllComponents(lifecycleMsg)
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

  private def sendAllComponents(lifecycleMsg: ToComponentLifecycleMessage): Unit = {
    supervisors.foreach { _.supervisor ! Lifecycle(lifecycleMsg) }
  }

  private def createComponents(componentInfos: Set[ComponentInfo]): Unit = {
    supervisors = supervisors ::: componentInfos.flatMap(createComponent).toList
    waitForRunningComponents()
  }

  private def createComponent(componentInfo: ComponentInfo): Option[SupervisorInfo] = {
    supervisors.find(_.componentInfo == componentInfo) match {
      case Some(_) => None
      case None =>
        val supervisor = ctx.spawn(SupervisorBehaviorFactory.make(componentInfo), componentInfo.name)
        Some(SupervisorInfo(supervisor, componentInfo))
    }
  }

  def waitForRunningComponents(): Unit = {
    mode = ContainerMode.Idle
    supervisors.foreach(
      _.supervisor ! LifecycleStateSubscription(Subscribe[LifecycleStateChanged](lifecycleChangeAdapterActor))
    )
  }

  def onLifecycleStateChanged(lifecycleStateChanged: LifecycleStateChanged): Any = {
    lifecycleStateChanged.publisher ! LifecycleStateSubscription(
      Unsubscribe[LifecycleStateChanged](lifecycleChangeAdapterActor)
    )
    runningComponents = (supervisors.find(_.supervisor == lifecycleStateChanged.publisher) ++ runningComponents).toList
    if (runningComponents.size == supervisors.size) {
      mode = ContainerMode.Running
      runningComponents = List.empty
    }
  }

  def onRestart(): Unit = {
    waitForRunningComponents()
    sendAllComponents(Restart)
  }

  def onShutdown(): Unit = {
    mode = ContainerMode.Idle
    unregisterFromLocationService()
    sendAllComponents(Shutdown)
  }

  def registerWithLocationService(): Unit = {}

  def unregisterFromLocationService(): Unit = {}
}
