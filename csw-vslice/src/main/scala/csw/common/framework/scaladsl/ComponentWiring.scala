package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.internal.ComponentBehavior
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{ComponentInfo, ComponentMsg, FromComponentLifecycleMessage}
import csw.param.states.CurrentState

import scala.reflect.ClassTag

abstract class ComponentWiring[Msg <: DomainMsg: ClassTag] {
  def handlers(
      ctx: ActorContext[ComponentMsg],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PublisherMsg[CurrentState]]
  ): ComponentHandlers[Msg]

  def compBehavior(
      compInfo: ComponentInfo,
      supervisor: ActorRef[FromComponentLifecycleMessage],
      pubSubRef: ActorRef[PublisherMsg[CurrentState]]
  ): Behavior[Nothing] =
    Actor
      .mutable[ComponentMsg](ctx ⇒ new ComponentBehavior[Msg](ctx, supervisor, handlers(ctx, compInfo, pubSubRef)))
      .narrow
}
