package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.{ComponentInfo, ComponentMessage, PubSub}
import csw.common.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.param.states.CurrentState

class SampleComponentBehaviorFactory extends ComponentBehaviorFactory[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]]
  ): ComponentHandlers[ComponentDomainMessage] = new SampleComponentHandlers(ctx, componentInfo, pubSubRef)
}

class ComponentBehaviorFactoryToSimulateFailure extends ComponentBehaviorFactory[ComponentDomainMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]]
  ): ComponentHandlers[ComponentDomainMessage] = new ComponentHandlerToSimulateFailure(ctx, componentInfo, pubSubRef)
}
