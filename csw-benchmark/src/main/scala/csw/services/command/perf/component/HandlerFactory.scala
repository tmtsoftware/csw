package csw.services.command.perf.component

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo

class HandlerFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      cswServices: CswServices
  ): ComponentHandlers =
    new ActionLessHandlers(ctx, componentInfo, cswServices)
}
