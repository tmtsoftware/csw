package csw.common.framework.scaladsl.assembly

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{ComponentMsg, FromComponentLifecycleMessage}
import csw.common.framework.scaladsl.ComponentBehaviorFactory

import scala.reflect.ClassTag

abstract class AssemblyBehaviorFactory[Msg <: DomainMsg: ClassTag] extends ComponentBehaviorFactory[AssemblyInfo] {
  protected def make(ctx: ActorContext[ComponentMsg], assemblyInfo: AssemblyInfo): AssemblyHandlers[Msg]

  def behavior(assemblyInfo: AssemblyInfo, supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] =
    Actor
      .mutable[ComponentMsg](ctx ⇒ new AssemblyBehavior[Msg](ctx, supervisor, make(ctx, assemblyInfo)))
      .narrow
}
