package csw.common.framework.scaladsl.hcd

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models.RunningHcdMsg.HcdDomainMsg
import csw.common.framework.models.{HcdMsg, HcdResponseMode}

import scala.reflect.ClassTag

abstract class HcdHandlersFactory[Msg <: HcdDomainMsg: ClassTag] {
  def make(ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo): HcdHandlers[Msg]

  def behavior(hcdInfo: HcdInfo, supervisor: ActorRef[HcdResponseMode]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ new HcdBehavior[Msg](ctx, supervisor, make(ctx, hcdInfo))).narrow
}
