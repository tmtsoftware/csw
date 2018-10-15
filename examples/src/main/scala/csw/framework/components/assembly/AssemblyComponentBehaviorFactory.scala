package csw.framework.components.assembly

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.command.client.internal.messages.TopLevelActorMessage

//#component-factory
class AssemblyComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new AssemblyComponentHandlers(ctx, cswCtx)
}
//#component-factory
