package example.framework.components.hcd

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.{ComponentContext, CswContext}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.command.client.messages.TopLevelActorMessage

//#component-factory
class HcdComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new HcdComponentHandlers(ctx, cswCtx)
}
//#component-factory
