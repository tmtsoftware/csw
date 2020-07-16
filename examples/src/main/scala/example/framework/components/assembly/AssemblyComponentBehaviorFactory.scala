package example.framework.components.assembly

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.{ComponentContext, CswContext}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.command.client.messages.TopLevelActorMessage

//#component-factory
class AssemblyComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new AssemblyComponentHandlers(ctx, cswCtx)
}
//#component-factory
