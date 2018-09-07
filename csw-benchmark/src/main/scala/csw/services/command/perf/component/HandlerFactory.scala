package csw.services.command.perf.component

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage

class HandlerFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage], cswServices: CswServices): ComponentHandlers =
    new ActionLessHandlers(ctx, cswServices)
}
