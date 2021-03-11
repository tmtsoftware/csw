package csw.framework.javadsl

import akka.actor.typed.javadsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.JCswContext
import csw.framework.scaladsl.ComponentHandlers

/**
 * Base class for component handlers which will be used by the component actor
 *
 * @param ctx the [[akka.actor.typed.javadsl.ActorContext]] under which the actor instance of the component, which use these handlers, is created
 * @param cswCtx provides access to csw services e.g. location, event, alarm, etc
 */
abstract class JComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: JCswContext)
    extends ComponentHandlers(ctx.asScala, cswCtx.asScala) {}
