package csw.common.framework.scaladsl.hcd

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.common.framework.models.{DomainMsg, HcdMsg, HcdResponseMode}

import scala.reflect.ClassTag

abstract class HcdActorFactory[Msg <: DomainMsg: ClassTag] {

  protected def make(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdResponseMode]): HcdActor[Msg]

  def behaviour(supervisor: ActorRef[HcdResponseMode]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ make(ctx, supervisor)).narrow
}
