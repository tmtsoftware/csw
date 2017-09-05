package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.internal.component.ComponentBehavior
import csw.common.framework.models.PubSub.PublisherMessage
import csw.common.framework.models.RunningMessage.DomainMessage
import csw.common.framework.models.{ComponentInfo, ComponentMessage, FromComponentLifecycleMessage}
import csw.param.states.CurrentState

import scala.reflect.ClassTag

abstract class ComponentBehaviorFactory[Msg <: DomainMessage: ClassTag] {

  def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PublisherMessage[CurrentState]]
  ): ComponentHandlers[Msg]

  def make(
      compInfo: ComponentInfo,
      supervisor: ActorRef[FromComponentLifecycleMessage],
      pubSubRef: ActorRef[PublisherMessage[CurrentState]]
  ): Behavior[Nothing] =
    Actor
      .mutable[ComponentMessage](ctx ⇒ new ComponentBehavior[Msg](ctx, supervisor, handlers(ctx, compInfo, pubSubRef)))
      .narrow
}
