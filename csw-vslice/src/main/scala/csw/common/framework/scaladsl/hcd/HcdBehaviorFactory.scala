package csw.common.framework.scaladsl.hcd

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{ComponentMsg, FromComponentLifecycleMessage}
import csw.common.framework.scaladsl.ComponentBehaviorFactory

import scala.reflect.ClassTag

abstract class HcdBehaviorFactory[Msg <: DomainMsg: ClassTag] extends ComponentBehaviorFactory[HcdInfo] {
  protected def make(ctx: ActorContext[ComponentMsg], hcdInfo: HcdInfo): HcdHandlers[Msg]

  def behavior(hcdInfo: HcdInfo, supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] =
    Actor.mutable[ComponentMsg](ctx ⇒ new HcdBehavior[Msg](ctx, supervisor, make(ctx, hcdInfo))).narrow
}
