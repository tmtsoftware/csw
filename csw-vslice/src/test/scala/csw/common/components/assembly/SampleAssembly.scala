package csw.common.components.assembly

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.common.ccs.Validation.Valid
import csw.common.ccs.{CommandStatus, Validation}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.{AssemblyComponentLifecycleMessage, AssemblyMsg, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.AssemblyActor
import csw.param.Parameters

import scala.concurrent.Future

class SampleAssembly(ctx: ActorContext[AssemblyMsg],
                     assemblyInfo: AssemblyInfo,
                     supervisor: ActorRef[AssemblyComponentLifecycleMessage])
    extends AssemblyActor[AssemblyDomainMessages](ctx, assemblyInfo, supervisor) {

  override def initialize(): Future[Unit] = Future.unit

  override def onRun(): Unit = Unit

  override def setup(s: Parameters.Setup,
                     commandOriginator: Option[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation = Valid

  override def observe(o: Parameters.Observe,
                       replyTo: Option[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation = Valid

  override def onLifecycle(message: ToComponentLifecycleMessage): Unit = Unit

  override def onDomainMsg(msg: AssemblyDomainMessages): Unit = Unit
}

object SampleAssembly {
  def behaviour(assemblyInfo: AssemblyInfo,
                supervisor: ActorRef[AssemblyComponentLifecycleMessage]): Behavior[AssemblyMsg] =
    Actor.mutable[AssemblyMsg](ctx ⇒ new SampleAssembly(ctx, assemblyInfo, supervisor)).narrow
}
