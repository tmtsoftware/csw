package csw.common.components.assembly

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.ccs.Validation.Valid
import csw.common.ccs.{CommandStatus, Validation}
import csw.common.framework.models.Component.{AssemblyInfo, HcdInfo}
import csw.common.framework.models._
import csw.common.framework.scaladsl.assembly.AssemblyActor
import csw.common.framework.scaladsl.hcd.HcdActorFactory
import csw.param.Parameters

import scala.concurrent.Future

class SampleAssembly(ctx: ActorContext[AssemblyMsg],
                     assemblyInfo: AssemblyInfo,
                     supervisor: ActorRef[AssemblyComponentLifecycleMessage])
    extends AssemblyActor[AssemblyDomainMessages](ctx, assemblyInfo, supervisor) {

  override def initialize(): Future[Unit] = Future.unit

  override def onRun(): Unit = ()

  override def setup(s: Parameters.Setup,
                     commandOriginator: Option[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation = Valid

  override def observe(o: Parameters.Observe,
                       replyTo: Option[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation = Valid

  override def onLifecycle(message: ToComponentLifecycleMessage): Unit = ()

  override def onDomainMsg(msg: AssemblyDomainMessages): Unit = ()
}

object SampleAssembly {
  def behaviour(assemblyInfo: AssemblyInfo,
                supervisor: ActorRef[AssemblyComponentLifecycleMessage]): Behavior[AssemblyMsg] = {
    val assemblyClass = Class.forName(assemblyInfo.componentClassName)
    val constructor =
      assemblyClass.getConstructor(classOf[ActorContext[AssemblyMsg]],
                                   classOf[AssemblyInfo],
                                   classOf[ActorRef[AssemblyComponentLifecycleMessage]])
    val assemblyFactory: (ActorContext[AssemblyMsg]) ⇒ MutableBehavior[AssemblyMsg] = (ctx: ActorContext[AssemblyMsg]) ⇒
      constructor.newInstance(ctx, assemblyInfo, supervisor).asInstanceOf[MutableBehavior[AssemblyMsg]]
    Actor.mutable[AssemblyMsg](assemblyFactory)
  }

  def behaviour(hcdInfo: HcdInfo, supervisor: ActorRef[HcdResponseMode]): Behavior[Nothing] = {
    val hcdClass   = Class.forName(hcdInfo.componentClassName + "Factory")
    val hcdFactory = hcdClass.newInstance().asInstanceOf[HcdActorFactory[_]]
    hcdFactory.behaviour(supervisor)
  }
}
