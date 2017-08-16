package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.{ComponentInfo, ComponentMsg, PubSub}
import csw.common.framework.scaladsl.{ComponentHandlers, ComponentWiring}
import csw.param.states.CurrentState

class SampleComponentWiring extends ComponentWiring[ComponentDomainMsg] {
  override def handlers(
      ctx: ActorContext[ComponentMsg],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMsg[CurrentState]]
  ): ComponentHandlers[ComponentDomainMsg] = new SampleComponentHandlers(ctx, componentInfo, pubSubRef)
}
