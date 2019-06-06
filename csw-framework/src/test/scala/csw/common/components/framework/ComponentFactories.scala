package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.{CommandAssemblyHandlers, CommandHcdHandlers}

class SampleComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      cswCtx: CswContext
  ): ComponentHandlers = new SampleComponentHandlers(ctx, cswCtx)
}

class CommandAssemblyBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      cswCtx: CswContext
  ): ComponentHandlers = new CommandAssemblyHandlers(ctx, cswCtx)
}

class CommandHcdBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      cswCtx: CswContext
  ): ComponentHandlers = new CommandHcdHandlers(ctx, cswCtx)
}

class ComponentBehaviorFactoryToSimulateFailure extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new ComponentHandlerToSimulateFailure(ctx, cswCtx)
}
