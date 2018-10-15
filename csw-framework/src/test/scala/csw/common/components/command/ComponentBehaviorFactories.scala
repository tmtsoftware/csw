package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.command.client.internal.messages.TopLevelActorMessage

class ComponentBehaviorFactoryForCommand extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new ComponentHandlerForCommand(ctx, cswCtx)
}

class McsAssemblyBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new McsAssemblyComponentHandlers(ctx, cswCtx)
}

class McsHcdBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new McsHcdComponentHandlers(ctx, cswCtx)
}
