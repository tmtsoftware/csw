package csw.framework.internal.component

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.command.messages.TopLevelActorMessage

class TestComponentBehaviorFactory(componentHandlers: ComponentHandlers) extends ComponentBehaviorFactory {
  override protected def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    componentHandlers
}
