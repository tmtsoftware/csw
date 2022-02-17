package csw.framework.scaladsl

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, javadsl}
import csw.command.client.messages.{FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.framework.internal.component.ComponentBehavior
import csw.framework.javadsl.{JComponentBehaviorFactory, JComponentHandlers}
import csw.framework.models.{CswContext, JCswContext}

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
  private val scalaConstructorArgs     = Seq(classOf[ActorContext[TopLevelActorMessage]], classOf[CswContext])
  private val javaConstructorArgs      = Seq(classOf[javadsl.ActorContext[TopLevelActorMessage]], classOf[JCswContext])
  private val requiredJavaConstructor  = getConstructorFor(classOf[JComponentHandlers], javaConstructorArgs)
  private val requiredScalaConstructor = getConstructorFor(classOf[ComponentHandlers], scalaConstructorArgs)

  def make(componentHandlerClassString: String): ComponentBehaviorFactory = {
    val componentHandlerClass = Class.forName(componentHandlerClassString)

    if (verifyHandler(componentHandlerClass, requiredJavaConstructor)) {
      val inputJavaConstructor = getConstructorFor(componentHandlerClass, javaConstructorArgs)
      val bf: JComponentBehaviorFactory = (ctx, cswCtx) =>
        inputJavaConstructor.newInstance(ctx, cswCtx).asInstanceOf[JComponentHandlers]
      bf
    }
    else if (verifyHandler(componentHandlerClass, requiredScalaConstructor)) {
      val inputScalaConstructor = getConstructorFor(componentHandlerClass, scalaConstructorArgs)
      (ctx, cswCtx) => inputScalaConstructor.newInstance(ctx, cswCtx).asInstanceOf[ComponentHandlers]
    }
    else
      throw new ClassCastException(s"""
         |To load a component, you must provide one of the following:
         |Child Class of ${classOf[ComponentHandlers]} having constructor parameter types:
         |(${classOf[ActorContext[TopLevelActorMessage]]}, ${classOf[CswContext]})
         |OR
         |Child Class of ${classOf[JComponentHandlers]} having constructor parameter types:
         |(${classOf[javadsl.ActorContext[TopLevelActorMessage]]}, ${classOf[JCswContext]}).
         |Received:
         |${componentHandlerClass.getDeclaredConstructors.last}
         |""".stripMargin)
  }

  private def verifyHandler(
      inputHandlerClass: Class[?],
      requiredConstructor: Constructor[?]
  ): Boolean = {
    // verify input class is assignable from ComponentHandler class && it's constructor has required parameters.
    requiredConstructor.getDeclaringClass.isAssignableFrom(inputHandlerClass) &&
    inputHandlerClass.getDeclaredConstructors.exists(constructor =>
      requiredConstructor.getParameterTypes.sameElements(constructor.getParameterTypes)
    )
  }

  private def getConstructorFor(clazz: Class[?], consArgs: Seq[Class[?]]): Constructor[?] = {
    val constructor = clazz.getDeclaredConstructor(consArgs*)
    constructor.setAccessible(true)
    constructor
  }

}
