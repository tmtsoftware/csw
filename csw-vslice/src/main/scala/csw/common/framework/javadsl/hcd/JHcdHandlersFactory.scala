package csw.common.framework.javadsl.hcd

import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models.{DomainMsg, HcdMsg, HcdResponseMode}
import csw.common.framework.scaladsl.hcd.HcdBehavior

import scala.reflect.ClassTag

abstract class JHcdHandlersFactory[Msg <: DomainMsg](klass: Class[Msg]) {
  def make(ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo): JHcdHandlers[Msg]

  def behaviour(supervisor: ActorRef[HcdResponseMode], hcdInfo: HcdInfo): Behavior[Nothing] =
    Actor
      .mutable[HcdMsg](ctx ⇒ new HcdBehavior[Msg](ctx, supervisor, make(ctx.asJava, hcdInfo))(ClassTag(klass)))
      .narrow
}
