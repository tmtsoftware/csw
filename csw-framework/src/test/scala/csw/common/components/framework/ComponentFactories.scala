package csw.common.components.framework

import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.{CommandAssemblyHandlers, CommandHcdHandlers}
import csw.framework.models.{ComponentContext, CswContext}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}

class SampleComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new SampleComponentHandlers(ctx, cswCtx)
}

class CommandAssemblyBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new CommandAssemblyHandlers(ctx, cswCtx)
}

class CommandHcdBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new CommandHcdHandlers(ctx, cswCtx)
}

class ComponentBehaviorFactoryToSimulateFailure extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new ComponentHandlerToSimulateFailure(ctx, cswCtx)
}
