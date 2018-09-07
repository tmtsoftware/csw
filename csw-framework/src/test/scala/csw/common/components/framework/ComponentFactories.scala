package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo

class SampleComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      cswServices: CswServices
  ): ComponentHandlers = new SampleComponentHandlers(ctx, componentInfo, cswServices)
}

class ComponentBehaviorFactoryToSimulateFailure extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      cswServices: CswServices
  ): ComponentHandlers = new ComponentHandlerToSimulateFailure(ctx, componentInfo, cswServices)
}
