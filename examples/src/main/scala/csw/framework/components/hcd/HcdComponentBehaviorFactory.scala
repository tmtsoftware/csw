package csw.framework.components.hcd

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo

//#component-factory
class HcdComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      cswServices: CswServices
  ): ComponentHandlers = new HcdComponentHandlers(ctx, componentInfo, cswServices)
}
//#component-factory
