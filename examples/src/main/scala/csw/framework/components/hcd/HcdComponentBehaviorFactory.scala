package csw.framework.components.hcd

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage

//#component-factory
class HcdComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswServices: CswServices): ComponentHandlers =
    new HcdComponentHandlers(ctx, cswServices)
}
//#component-factory
