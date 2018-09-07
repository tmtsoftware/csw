package csw.apps.clusterseed.components

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.CurrentStatePublisher
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo
import csw.services.command.CommandResponseManager

class GalilBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      cswServices: CswServices
  ): ComponentHandlers =
    new GalilComponentHandlers(ctx, componentInfo, cswServices)
}
