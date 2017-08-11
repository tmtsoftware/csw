package csw.common.framework.javadsl

import akka.typed.javadsl.ActorContext
import akka.typed.{scaladsl, ActorRef}
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models.{ComponentInfo, ComponentMsg, PubSub}
import csw.common.framework.scaladsl.{ComponentHandlers, ComponentWiring}
import csw.param.states.CurrentState

import scala.reflect.ClassTag

abstract class JComponentWiring[Msg <: DomainMsg](klass: Class[Msg]) extends ComponentWiring[Msg]()(ClassTag(klass)) {

  def make(ctx: ActorContext[ComponentMsg],
           componentInfo: ComponentInfo,
           pubSubRef: ActorRef[PubSub.PublisherMsg[CurrentState]]): JComponentHandlers[Msg]

  def handlers(ctx: scaladsl.ActorContext[ComponentMsg],
               componentInfo: ComponentInfo,
               pubSubRef: ActorRef[PubSub.PublisherMsg[CurrentState]]): ComponentHandlers[Msg] =
    make(ctx.asJava, componentInfo, pubSubRef)
}
