package csw.common.framework.scaladsl.hcd

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.common.framework.models.{DomainMsg, HcdMsg, HcdResponseMode}

import scala.reflect.ClassTag

abstract class HcdHandlersFactory[Msg <: DomainMsg: ClassTag] {
  def make(ctx: ActorContext[HcdMsg]): HcdHandlers[Msg]

  def behaviour(supervisor: ActorRef[HcdResponseMode]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ new HcdBehavior[Msg](ctx, supervisor, make(ctx))).narrow
}
