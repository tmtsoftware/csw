package csw.common.framework.scaladsl.assembly

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.{ComponentMsg, ComponentResponseMode}
import csw.common.framework.models.RunningMsg.DomainMsg

import scala.reflect.ClassTag

abstract class AssemblyHandlersFactory[Msg <: DomainMsg: ClassTag] {
  def make(ctx: ActorContext[ComponentMsg], assemblyInfo: AssemblyInfo): AssemblyHandlers[Msg]

  def behavior(assemblyInfo: AssemblyInfo, supervisor: ActorRef[ComponentResponseMode]): Behavior[Nothing] =
    Actor
      .mutable[ComponentMsg](ctx ⇒ new AssemblyBehavior[Msg](ctx, supervisor, make(ctx, assemblyInfo)))
      .narrow
}
