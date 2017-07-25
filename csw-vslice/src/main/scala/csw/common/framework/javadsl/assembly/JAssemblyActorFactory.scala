package csw.common.framework.javadsl.assembly

import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.{AssemblyComponentLifecycleMessage, AssemblyMsg, DomainMsg}

abstract class JAssemblyActorFactory[Msg <: DomainMsg](klass: Class[Msg]) {
  def make(ctx: ActorContext[AssemblyMsg],
           assemblyInfo: AssemblyInfo,
           supervisor: ActorRef[AssemblyComponentLifecycleMessage]): JAssemblyActor[Msg]

  def behaviour(assemblyInfo: AssemblyInfo,
                supervisor: ActorRef[AssemblyComponentLifecycleMessage]): Behavior[Nothing] = {
    Actor.mutable[AssemblyMsg](ctx ⇒ make(ctx.asJava, assemblyInfo, supervisor)).narrow
  }
}
