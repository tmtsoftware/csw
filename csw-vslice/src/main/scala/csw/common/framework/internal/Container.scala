package csw.common.framework.internal

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.CommonSupervisorMsg.LifecycleStateSubscription
import csw.common.framework.models.ComponentInfo.ContainerInfo
import csw.common.framework.models.ContainerMsg.{
  CreateComponents,
  CreationDelayCompleted,
  GetComponents,
  GoOffline,
  GoOnline,
  LifecycleStateChanged,
  LifecycleToAll,
  Restart,
  Shutdown
}
import csw.common.framework.models.PubSub.{Subscribe, Unsubscribe}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models._
import csw.common.framework.scaladsl.SupervisorBehaviorFactory
import csw.services.location.models.{ComponentId, RegistrationResult}

class Container(ctx: ActorContext[ContainerMsg], containerInfo: ContainerInfo) extends MutableBehavior[ContainerMsg] {
  val componentInfos: Set[ComponentInfo] = containerInfo.componentInfos
  val name: String                       = containerInfo.componentName
  val componentId                        = ComponentId(name, containerInfo.componentType)

  var supervisors: List[SupervisorInfo]           = List.empty
  var restarted: List[SupervisorInfo]             = List.empty
  var registrationOpt: Option[RegistrationResult] = None
  var mode: ContainerMode                         = ContainerMode.Idle

  registerWithLocationService()

  ctx.self ! CreateComponents(containerInfo.componentInfos)

  def onRestart(): Unit = {
    mode = ContainerMode.Restart
    supervisors.foreach(_.supervisor ! LifecycleStateSubscription(Subscribe[LifecycleStateChanged](ctx.self)))
    sendAllComponents(Lifecycle(ToComponentLifecycleMessage.Restart))
  }

  def onLifecycleStateChanged(publisher: ActorRef[SupervisorMsg]): Any = {
    publisher ! LifecycleStateSubscription(Unsubscribe[LifecycleStateChanged](ctx.self))
    val reloaded: List[SupervisorInfo] = (supervisors.find(_.supervisor == publisher) ++ restarted).toList
    if (reloaded.size == supervisors.size) {
      mode = ContainerMode.Running
      restarted = List.empty
    }
  }

  override def onMessage(msg: ContainerMsg): Behavior[ContainerMsg] = {
    (mode, msg) match {
      case (ContainerMode.Running, GetComponents(replyTo))  ⇒ replyTo ! Components(supervisors)
      case (ContainerMode.Running, Shutdown)                ⇒ sendAllComponents(Lifecycle(ToComponentLifecycleMessage.Shutdown))
      case (ContainerMode.Running, GoOnline)                ⇒ sendAllComponents(Lifecycle(ToComponentLifecycleMessage.GoOnline))
      case (ContainerMode.Running, GoOffline)               ⇒ sendAllComponents(Lifecycle(ToComponentLifecycleMessage.GoOffline))
      case (ContainerMode.Running, Restart)                 ⇒ onRestart()
      case (ContainerMode.Running, CreateComponents(infos)) ⇒ createComponents(infos)
      case (ContainerMode.Running, LifecycleToAll(cmd))     ⇒ sendAllComponents(cmd)
      case (ContainerMode.Restart, LifecycleStateChanged(SupervisorMode.Running, publisher)) ⇒
        onLifecycleStateChanged(publisher)
      case (containerMode, message) ⇒ println(s"Container in $containerMode received an unexpected message: $message")
    }
    this
  }

  private def sendAllComponents(cmd: SupervisorExternalMessage): Unit = {
    supervisors.foreach { info ⇒
      info.supervisor ! cmd
      ctx.schedule(containerInfo.creationDelay, ctx.self, CreationDelayCompleted)
    }
  }

  private def createComponents(cinfos: Set[ComponentInfo]): Unit = {
    supervisors = supervisors ::: cinfos.flatMap(createComponent(_, supervisors)).toList
    mode = ContainerMode.Running
  }

  private def createComponent(componentInfo: ComponentInfo,
                              supervisors: List[SupervisorInfo]): Option[SupervisorInfo] = {
    supervisors.find(_.componentInfo == componentInfo) match {
      case Some(existingComponentInfo) =>
        None
      case None =>
        val supervisor = ctx.spawn(SupervisorBehaviorFactory.make(componentInfo), componentInfo.componentName)
        Some(SupervisorInfo(supervisor, componentInfo))
    }
  }

  def registerWithLocationService(): Unit = ???

  private def unregisterFromLocationService(): Unit = ???
}
