package csw.common.framework.javadsl.assembly

import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{ComponentMsg, FromComponentLifecycleMessage}
import csw.common.framework.scaladsl.assembly.AssemblyBehavior

import scala.reflect.ClassTag

abstract class JAssemblyHandlersFactory[Msg <: DomainMsg](klass: Class[Msg]) {
  def make(ctx: ActorContext[ComponentMsg], assemblyInfo: AssemblyInfo): JAssemblyHandlers[Msg]

  def behavior(assemblyInfo: AssemblyInfo, supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] = {
    Actor
      .mutable[ComponentMsg](
        ctx ⇒ new AssemblyBehavior[Msg](ctx, supervisor, make(ctx.asJava, assemblyInfo))(ClassTag(klass))
      )
      .narrow
  }
}
