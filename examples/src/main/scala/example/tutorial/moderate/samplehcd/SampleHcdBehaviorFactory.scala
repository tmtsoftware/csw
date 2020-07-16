package example.tutorial.moderate.samplehcd

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.{ComponentContext, CswContext}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}

class SampleHcdBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new SampleHcdHandlers(ctx, cswCtx)
}
