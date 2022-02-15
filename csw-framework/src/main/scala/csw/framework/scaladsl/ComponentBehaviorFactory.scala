package csw.framework.scaladsl

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, javadsl}
import csw.command.client.messages.{FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.framework.internal.component.ComponentBehavior
import csw.framework.javadsl.{JComponentBehaviorFactory, JComponentHandlers}
import csw.framework.models.{ComponentHandlerNotFoundException, CswContext, JCswContext}

import java.lang.reflect.Constructor

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

object ComponentBehaviorFactory {

  def make(componentHandlerClassString: String): ComponentBehaviorFactory = {
    val componentHandlerClass = Class.forName(componentHandlerClassString)
    if (verifyJavaHandler(componentHandlerClass)) {
      val jComponentBehaviorF: JComponentBehaviorFactory = (ctx, cswCtx) =>
        javaHandlerConstructor(componentHandlerClass).newInstance(ctx, cswCtx).asInstanceOf[JComponentHandlers]
      jComponentBehaviorF
    }
    else if (verifyScalaHandler(componentHandlerClass)) {
      val componentBehaviorF: ComponentBehaviorFactory = (ctx, cswCtx) =>
        scalaHandlerConstructor(componentHandlerClass).newInstance(ctx, cswCtx).asInstanceOf[ComponentHandlers]
      componentBehaviorF
    }
    else throw new ComponentHandlerNotFoundException
  }

  private def verifyJavaHandler(componentHandlerClass: Class[?]): Boolean =
    componentHandlerClass.getDeclaredConstructors.exists(constructor => {
      javaHandlerConstructor(classOf[JComponentHandlers]).getParameterTypes.sameElements(constructor.getParameterTypes)
    })

  private def verifyScalaHandler(componentHandlerClass: Class[?]): Boolean =
    componentHandlerClass.getDeclaredConstructors.exists(constructor => {
      scalaHandlerConstructor(classOf[ComponentHandlers]).getParameterTypes.sameElements(constructor.getParameterTypes)
    })

  private def scalaHandlerConstructor(clazz: Class[?]): Constructor[?] =
    clazz
      .getDeclaredConstructor(classOf[ActorContext[TopLevelActorMessage]], classOf[CswContext])

  private def javaHandlerConstructor(clazz: Class[?]): Constructor[?] = {
    val constructor = clazz
      .getDeclaredConstructor(classOf[javadsl.ActorContext[TopLevelActorMessage]], classOf[JCswContext])
    constructor.setAccessible(true)
    constructor
  }

}
