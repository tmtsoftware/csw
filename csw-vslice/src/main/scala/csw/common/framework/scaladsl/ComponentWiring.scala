package csw.common.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.internal.ComponentBehavior
import csw.common.framework.internal.supervisor.Supervisor
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{ComponentMsg, FromComponentLifecycleMessage, SupervisorMsg}
import csw.param.states.CurrentState

import scala.reflect.ClassTag

object ComponentWiring {
  def supervisorBehavior(componentInfo: ComponentInfo): Behavior[SupervisorMsg] = {
    val componentWiringClass = Class.forName(componentInfo.componentClassName)
    val compWring            = componentWiringClass.newInstance().asInstanceOf[ComponentWiring[_]]
    Actor.mutable[SupervisorMsg](ctx => new Supervisor(ctx, componentInfo, compWring))
  }
}

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
