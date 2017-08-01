package csw.common.framework.scaladsl.assembly

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.RunningAssemblyMsg.AssemblyDomainMsg
import csw.common.framework.models.{AssemblyMsg, AssemblyResponseMode}

import scala.reflect.ClassTag

abstract class AssemblyHandlersFactory[Msg <: AssemblyDomainMsg: ClassTag] {
  def make(ctx: ActorContext[AssemblyMsg], assemblyInfo: AssemblyInfo): AssemblyHandlers[Msg]

  def behavior(assemblyInfo: AssemblyInfo, supervisor: ActorRef[AssemblyResponseMode]): Behavior[Nothing] =
    Actor
      .mutable[AssemblyMsg](ctx ⇒ new AssemblyBehavior[Msg](ctx, supervisor, make(ctx, assemblyInfo)))
      .narrow
}
