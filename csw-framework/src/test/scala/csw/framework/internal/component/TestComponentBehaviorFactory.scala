package csw.framework.internal.component

import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.{ComponentContext, CswContext}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}

class TestComponentBehaviorFactory(componentHandlers: ComponentHandlers) extends ComponentBehaviorFactory {
  override protected def handlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    componentHandlers
}
