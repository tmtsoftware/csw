package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.{ComponentInfo, ComponentMessage, PubSub}
import csw.common.framework.scaladsl.{ComponentHandlers, ComponentWiring}
import csw.param.states.CurrentState

class SampleComponentWiring extends ComponentWiring[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]]
  ): ComponentHandlers[ComponentDomainMessage] = new SampleComponentHandlers(ctx, componentInfo, pubSubRef)
}

class ComponentWiringToSimulateFailure extends ComponentWiring[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]]
  ): ComponentHandlers[ComponentDomainMessage] = new ComponentHandlerToSimulateFailure(ctx, componentInfo, pubSubRef)
}
