package csw.framework.components.assembly

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo

//#component-factory
class AssemblyComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      cswServices: CswServices
  ): ComponentHandlers =
    new AssemblyComponentHandlers(ctx, componentInfo, cswServices)
}
//#component-factory
