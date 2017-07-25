package csw.common.framework.javadsl.hcd

import akka.typed.scaladsl.Actor
import akka.typed.{javadsl, ActorRef, Behavior}
import csw.common.framework.models.{DomainMsg, HcdMsg, HcdResponseMode}

abstract class JHcdActorFactory[Msg <: DomainMsg](klass: Class[Msg]) {
  def make(ctx: javadsl.ActorContext[HcdMsg], supervisor: ActorRef[HcdResponseMode]): JHcdActor[Msg]

  def behaviour(supervisor: ActorRef[HcdResponseMode]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ make(ctx.asJava, supervisor)).narrow
}
