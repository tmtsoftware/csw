/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.scaladsl

import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, javadsl}
import csw.command.client.messages.{FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.framework.internal.component.ComponentBehavior
import csw.framework.javadsl.{JComponentHandlers, JComponentHandlersFactory}
import csw.framework.models.{CswContext, JCswContext}

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
// The annotation is required to prevent a warning while interpreting a lambda into this SAM interface
@FunctionalInterface
private[framework] abstract class ComponentHandlersFactory {

  /**
   * Implement this method for providing the component handlers to be used by component actor
   *
   * @param ctx the [[pekko.actor.typed.scaladsl.ActorContext]] under which the actor instance of this behavior is created
   * @param cswCtx provides access to csw services e.g. location, event, alarm, etc
   * @return componentHandlers to be used by this component
   */
  def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers

  /**
   * Creates the [[pekko.actor.typed.Behavior]] of the component
   *
   * @param supervisor the actor reference of the supervisor actor which created this component for this component
   * @param cswCtx provides access to csw services e.g. location, event, alarm, etc
   * @return behavior for component Actor
   */
  private[framework] def make(supervisor: ActorRef[FromComponentLifecycleMessage], cswCtx: CswContext): Behavior[Nothing] =
    Behaviors
      .setup[TopLevelActorMessage] { ctx => ComponentBehavior.make(supervisor, handlers(ctx, cswCtx), cswCtx) }
      .narrow
}

private[framework] object ComponentHandlersFactory {
  def make(componentHandlerClassPath: String): ComponentHandlersFactory = {
    val inputClass = new InputClass(Class.forName(componentHandlerClassPath))

    if (inputClass.isValid[JComponentHandlers]) {
      // type scription is required to interpret the lambda as a SAM interface
      inputClass.instantiateAs[JComponentHandlers]: JComponentHandlersFactory
    }
    else if (inputClass.isValid[ComponentHandlers]) {
      // type scription is required to interpret the lambda as a SAM interface
      inputClass.instantiateAs[ComponentHandlers]: ComponentHandlersFactory
    }
    else
      throw new ClassCastException(
        s"""
         |To load a component, you must provide one of the following:
         |For Scala: Subclass of ${classOf[ComponentHandlers]} having constructor parameter types:
         |(${classOf[ActorContext[TopLevelActorMessage]]}, ${classOf[CswContext]})
         |OR
         |For Java: Subclass of ${classOf[JComponentHandlers]} having constructor parameter types:
         |(${classOf[javadsl.ActorContext[TopLevelActorMessage]]}, ${classOf[JCswContext]}).
         |Received:
         |${inputClass.inputClass.getDeclaredConstructors.last}
         |""".stripMargin
      )
  }

}
