package csw.common.framework.javadsl

import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{ComponentMsg, FromComponentLifecycleMessage}
import csw.common.framework.scaladsl.ComponentBehavior

import scala.reflect.ClassTag

abstract class JComponentBehaviorFactory[Msg <: DomainMsg](klass: Class[Msg]) {
  def make(ctx: ActorContext[ComponentMsg], componentInfo: ComponentInfo): JComponentHandlers[Msg]

  def behavior(componentInfo: ComponentInfo, supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] =
    Actor
      .mutable[ComponentMsg](
        ctx ⇒ new ComponentBehavior[Msg](ctx, supervisor, make(ctx.asJava, componentInfo))(ClassTag(klass))
      )
      .narrow
}
