package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.internal.ComponentBehavior
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{ComponentMsg, FromComponentLifecycleMessage}
import csw.param.StateVariable.CurrentState

import scala.reflect.ClassTag

abstract class ComponentBehaviorFactory[Msg <: DomainMsg: ClassTag] {
  protected def make(ctx: ActorContext[ComponentMsg],
                     componentInfo: ComponentInfo,
                     pubSubRef: ActorRef[PublisherMsg[CurrentState]]): ComponentHandlers[Msg]

  def behavior(compInfo: ComponentInfo,
               supervisor: ActorRef[FromComponentLifecycleMessage],
               pubSubRef: ActorRef[PublisherMsg[CurrentState]]): Behavior[Nothing] =
    Actor
      .mutable[ComponentMsg](ctx ⇒ new ComponentBehavior[Msg](ctx, supervisor, make(ctx, compInfo, pubSubRef)))
      .narrow

}
