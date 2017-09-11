package csw.common.framework.javadsl

import akka.typed.javadsl.ActorContext
import akka.typed.{scaladsl, ActorRef}
import csw.common.framework.models.RunningMessage.DomainMessage
import csw.common.framework.models.{ComponentInfo, ComponentMessage, PubSub}
import csw.common.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.param.states.CurrentState

import scala.reflect.ClassTag

abstract class JComponentBehaviorFactory[Msg <: DomainMessage](
    klass: Class[Msg]
) extends ComponentBehaviorFactory[Msg]()(ClassTag(klass)) {

  def handlers(
      ctx: scaladsl.ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]]
  ): ComponentHandlers[Msg] =
    make(ctx.asJava, componentInfo, pubSubRef)

  def make(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]]
  ): JComponentHandlers[Msg]
}
