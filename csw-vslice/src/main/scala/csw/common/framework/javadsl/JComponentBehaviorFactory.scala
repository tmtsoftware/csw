package csw.common.framework.javadsl

import akka.typed.javadsl.ActorContext
import akka.typed.{scaladsl, ActorRef}
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{ComponentMsg, PubSub}
import csw.common.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.param.states.CurrentState

import scala.reflect.ClassTag

abstract class JComponentBehaviorFactory[Msg <: DomainMsg](klass: Class[Msg])
    extends ComponentBehaviorFactory[Msg]()(ClassTag(klass)) {

  def make(ctx: ActorContext[ComponentMsg],
           componentInfo: ComponentInfo,
           pubSubRef: ActorRef[PubSub.PublisherMsg[CurrentState]]): JComponentHandlers[Msg]

  def make(ctx: scaladsl.ActorContext[ComponentMsg],
           componentInfo: ComponentInfo,
           pubSubRef: ActorRef[PubSub.PublisherMsg[CurrentState]]): ComponentHandlers[Msg] =
    make(ctx.asJava, componentInfo, pubSubRef)
}
