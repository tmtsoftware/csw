package csw.common.framework.scaladsl

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.{AssemblyComponentLifecycleMessage, AssemblyMsg, DomainMsg}

import scala.reflect.ClassTag

abstract class AssemblyActorFactory[Msg <: DomainMsg: ClassTag] {
  def make(ctx: ActorContext[AssemblyMsg],
           assemblyInfo: AssemblyInfo,
           supervisor: ActorRef[AssemblyComponentLifecycleMessage]): AssemblyActor[Msg]

  def behaviour(assemblyInfo: AssemblyInfo,
                supervisor: ActorRef[AssemblyComponentLifecycleMessage]): Behavior[Nothing] =
    Actor.mutable[AssemblyMsg](ctx ⇒ make(ctx, assemblyInfo, supervisor)).narrow
}
