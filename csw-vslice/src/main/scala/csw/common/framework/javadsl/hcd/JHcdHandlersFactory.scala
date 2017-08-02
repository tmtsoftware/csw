package csw.common.framework.javadsl.hcd

import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{ComponentMsg, FromComponentLifecycleMessage}
import csw.common.framework.scaladsl.hcd.HcdBehavior

import scala.reflect.ClassTag

abstract class JHcdHandlersFactory[Msg <: DomainMsg](klass: Class[Msg]) {
  def make(ctx: ActorContext[ComponentMsg], hcdInfo: HcdInfo): JHcdHandlers[Msg]

  def behavior(hcdInfo: HcdInfo, supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] =
    Actor
      .mutable[ComponentMsg](ctx ⇒ new HcdBehavior[Msg](ctx, supervisor, make(ctx.asJava, hcdInfo))(ClassTag(klass)))
      .narrow
}
