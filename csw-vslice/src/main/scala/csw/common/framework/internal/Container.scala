package csw.common.framework.internal

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior, PostStop, Signal}
import csw.common.framework.models.CommonSupervisorMsg.LifecycleStateSubscription
import csw.common.framework.models.ContainerMsg.{GetComponents, LifecycleStateChanged}
import csw.common.framework.models.PubSub.{Subscribe, Unsubscribe}
import csw.common.framework.models.RunningMsg.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.Restart
import csw.common.framework.models._
import csw.common.framework.scaladsl.SupervisorBehaviorFactory
import csw.services.location.models.{ComponentId, RegistrationResult}

class Container(ctx: ActorContext[ContainerMsg], containerInfo: ComponentInfo) extends MutableBehavior[ContainerMsg] {
  val componentId                                 = ComponentId(containerInfo.componentName, containerInfo.componentType)
  var supervisors: List[SupervisorInfo]           = List.empty
  var runningComponents: List[SupervisorInfo]     = List.empty
  var mode: ContainerMode                         = ContainerMode.Initialize
  var registrationOpt: Option[RegistrationResult] = None

  registerWithLocationService()

  createComponents(containerInfo.maybeComponentInfoes.get)

  override def onMessage(msg: ContainerMsg): Behavior[ContainerMsg] = {
    (mode, msg) match {
      case (_, GetComponents(replyTo))                      ⇒ replyTo ! Components(supervisors)
      case (ContainerMode.Running, Lifecycle(Restart))      ⇒ onRestart()
      case (ContainerMode.Running, Lifecycle(lifecycleMsg)) ⇒ sendAllComponents(lifecycleMsg)
      case (ContainerMode.Initialize, LifecycleStateChanged(SupervisorMode.Running, publisher)) ⇒
        onLifecycleStateChanged(publisher)
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
        val supervisor = ctx.spawn(SupervisorBehaviorFactory.make(componentInfo), componentInfo.componentName)
        Some(SupervisorInfo(supervisor, componentInfo))
    }
  }

  def waitForRunningComponents(): Unit = {
    mode = ContainerMode.Initialize
    supervisors.foreach(_.supervisor ! LifecycleStateSubscription(Subscribe[LifecycleStateChanged](ctx.self)))
  }

  def onLifecycleStateChanged(publisher: ActorRef[SupervisorMsg]): Any = {
    publisher ! LifecycleStateSubscription(Unsubscribe[LifecycleStateChanged](ctx.self))
    runningComponents = (supervisors.find(_.supervisor == publisher) ++ runningComponents).toList
    if (runningComponents.size == supervisors.size) {
      mode = ContainerMode.Running
      runningComponents = List.empty
    }
  }

  def onRestart(): Unit = {
    waitForRunningComponents()
    sendAllComponents(Restart)
  }

  def registerWithLocationService(): Unit = {}

  def unregisterFromLocationService(): Unit = {}
}
