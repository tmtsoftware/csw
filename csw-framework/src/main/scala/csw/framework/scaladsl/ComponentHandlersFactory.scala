package csw.framework.scaladsl

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, javadsl}
import csw.command.client.messages.{FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.framework.internal.component.ComponentBehavior
import csw.framework.javadsl.{JComponentHandlersFactory, JComponentHandlers}
import csw.framework.models.{CswContext, JCswContext}

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
private[framework] abstract class ComponentHandlersFactory {

  /**
   * Implement this method for providing the component handlers to be used by component actor
   *
   * @param ctx the [[akka.actor.typed.scaladsl.ActorContext]] under which the actor instance of this behavior is created
   * @param cswCtx provides access to csw services e.g. location, event, alarm, etc
   * @return componentHandlers to be used by this component
   */
  def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers

  /**
   * Creates the [[akka.actor.typed.Behavior]] of the component
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
  private val componentHandlerArgsType     = Seq(classOf[ActorContext[TopLevelActorMessage]], classOf[CswContext])
  private val componentHandlersConstructor = ClassHelpers.getConstructorFor(classOf[ComponentHandlers], componentHandlerArgsType)

  def make(componentHandlerClassPath: String): ComponentHandlersFactory = {
    val componentHandlerClass: Class[_] = Class.forName(componentHandlerClassPath)

    if (JComponentHandlersFactory.isValid(componentHandlerClass)) {
      JComponentHandlersFactory.make(componentHandlerClass)
    }
    else if (ComponentHandlersFactory.isValid(componentHandlerClass)) {
      ComponentHandlersFactory.make(componentHandlerClass)
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
         |${componentHandlerClass.getDeclaredConstructors.last}
         |""".stripMargin
      )
  }

  // verify input class is assignable from ComponentHandler class && it's constructor has required parameters.
  private def isValid(handlerClass: Class[?]): Boolean = ClassHelpers.verifyClass(handlerClass, componentHandlersConstructor)

  def make(componentHandlerClass: Class[?]): ComponentHandlersFactory = { (ctx, cswCtx) =>
    ClassHelpers
      .getConstructorFor(componentHandlerClass, componentHandlerArgsType)
      .newInstance(ctx, cswCtx)
      .asInstanceOf[ComponentHandlers]
  }
}
