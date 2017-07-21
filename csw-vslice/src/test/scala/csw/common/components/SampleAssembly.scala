package csw.common.components

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.common.ccs.Validation.Valid
import csw.common.ccs.{CommandStatus, Validation}
import csw.common.framework.Component.AssemblyInfo
import csw.common.framework._
import csw.param.Parameters

import scala.concurrent.Future

sealed trait SampleAssemblyMessage extends DomainMsg

class SampleAssembly(ctx: ActorContext[AssemblyMsg],
                     assemblyInfo: AssemblyInfo,
                     supervisor: ActorRef[AssemblyComponentLifecycleMessage])
    extends AssemblyActor[SampleAssemblyMessage](ctx, assemblyInfo, supervisor) {

  override def initialize(): Future[Unit] = Future.unit

  override def onRun(): Unit = Unit

  override def setup(s: Parameters.Setup,
                     commandOriginator: Option[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation = Valid

  override def observe(o: Parameters.Observe,
                       replyTo: Option[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation = Valid

  override def onLifecycle(message: ToComponentLifecycleMessage): Unit = Unit

  override def onDomainMsg(msg: SampleAssemblyMessage): Unit = Unit
}

object SampleAssembly {
  def behaviour(assemblyInfo: AssemblyInfo,
                supervisor: ActorRef[AssemblyComponentLifecycleMessage]): Behavior[Nothing] =
    Actor.mutable[AssemblyMsg](ctx ⇒ new SampleAssembly(ctx, assemblyInfo, supervisor)).narrow
}
