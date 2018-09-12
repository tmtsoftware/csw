package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage

class SampleComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      cswCtx: CswContext
  ): ComponentHandlers = new SampleComponentHandlers(ctx, cswCtx)
}

class ComponentBehaviorFactoryToSimulateFailure extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new ComponentHandlerToSimulateFailure(ctx, cswCtx)
}
