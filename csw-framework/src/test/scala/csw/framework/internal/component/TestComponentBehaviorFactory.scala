package csw.framework.internal.component

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage

class TestComponentBehaviorFactory(componentHandlers: ComponentHandlers) extends ComponentBehaviorFactory {
  override protected def handlers(ctx: ActorContext[TopLevelActorMessage], cswServices: CswServices): ComponentHandlers =
    componentHandlers
}
