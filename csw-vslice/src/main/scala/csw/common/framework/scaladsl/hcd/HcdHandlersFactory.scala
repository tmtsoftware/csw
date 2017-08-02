package csw.common.framework.scaladsl.hcd

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models.{ComponentMsg, ComponentResponseMode}
import csw.common.framework.models.RunningMsg.DomainMsg

import scala.reflect.ClassTag

abstract class HcdHandlersFactory[Msg <: DomainMsg: ClassTag] {

  def make(ctx: ActorContext[ComponentMsg], hcdInfo: HcdInfo): HcdHandlers[Msg]

  def behavior(hcdInfo: HcdInfo, supervisor: ActorRef[ComponentResponseMode]): Behavior[Nothing] =
    Actor.mutable[ComponentMsg](ctx ⇒ new HcdBehavior[Msg](ctx, supervisor, make(ctx, hcdInfo))).narrow
}
