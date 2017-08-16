package csw.common.components.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.{ComponentInfo, ComponentMsg, PubSub}
import csw.common.framework.scaladsl.{ComponentHandlers, ComponentWiring}
import csw.param.states.CurrentState

class SampleHcdWiring extends ComponentWiring[HcdDomainMsg] {
  override def handlers(
      ctx: ActorContext[ComponentMsg],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMsg[CurrentState]]
  ): ComponentHandlers[HcdDomainMsg] = new SampleHcdHandlers(ctx, componentInfo, pubSubRef)
}
