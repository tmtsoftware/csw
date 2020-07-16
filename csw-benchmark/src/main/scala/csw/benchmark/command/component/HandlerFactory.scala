package csw.benchmark.command.component

import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.{ComponentContext, CswContext}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}

class HandlerFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new ActionLessHandlers(ctx, cswCtx)
}
