package csw.common.framework.javadsl.assembly

import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.{AssemblyMsg, AssemblyResponseMode, DomainMsg}
import csw.common.framework.scaladsl.assembly.AssemblyBehavior

import scala.reflect.ClassTag

abstract class JAssemblyHandlersFactory[Msg <: DomainMsg](klass: Class[Msg]) {
  def make(ctx: ActorContext[AssemblyMsg], assemblyInfo: AssemblyInfo): JAssemblyHandlers[Msg]

  def behaviour(assemblyInfo: AssemblyInfo, supervisor: ActorRef[AssemblyResponseMode]): Behavior[Nothing] = {
    Actor
      .mutable[AssemblyMsg](
        ctx ⇒ new AssemblyBehavior[Msg](ctx, supervisor, make(ctx.asJava, assemblyInfo))(ClassTag(klass))
      )
      .narrow
  }
}
