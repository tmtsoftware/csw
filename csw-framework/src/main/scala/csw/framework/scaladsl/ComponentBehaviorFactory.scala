package csw.framework.scaladsl

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.messages.{FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.framework.internal.component.ComponentBehavior
import csw.framework.models.CswContext

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
abstract class ComponentBehaviorFactory {

  /**
   * Implement this method for providing the component handlers to be used by component actor
   *
   * @param ctx the [[akka.actor.typed.scaladsl.ActorContext]] under which the actor instance of this behavior is created
   * @param cswCtx provides access to csw services e.g. location, event, alarm, etc
   * @return componentHandlers to be used by this component
   */
  protected def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers

  /**
   * Creates the [[akka.actor.typed.scaladsl.MutableBehavior]] of the component
   *
   * @param supervisor the actor reference of the supervisor actor which created this component for this component
   * @param cswCtx provides access to csw services e.g. location, event, alarm, etc
   * @return behavior for component Actor
   */
  private[framework] def make(supervisor: ActorRef[FromComponentLifecycleMessage], cswCtx: CswContext): Behavior[Nothing] =
    Behaviors
      .setup[TopLevelActorMessage] { ctx ⇒
        new ComponentBehavior(ctx, supervisor, handlers(ctx, cswCtx), cswCtx)
      }
      .narrow
}
