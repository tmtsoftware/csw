package csw.common.components.command

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage

class ComponentBehaviorFactoryForCommand extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswServices: CswServices): ComponentHandlers =
    new ComponentHandlerForCommand(ctx, cswServices)
}

class McsAssemblyBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswServices: CswServices): ComponentHandlers =
    new McsAssemblyComponentHandlers(ctx, cswServices)
}

class McsHcdBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswServices: CswServices): ComponentHandlers =
    new McsHcdComponentHandlers(ctx, cswServices)
}
