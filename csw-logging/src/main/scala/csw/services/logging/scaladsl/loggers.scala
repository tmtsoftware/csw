package csw.services.logging.scaladsl

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import akka.typed.scaladsl.adapter.TypedActorRefOps
import akka.typed.scaladsl.{Actor, ActorContext}

/**
 * Extend this object to obtain a reference to a Simple or Actor Logger without a component name
 */
object GenericLogger extends BasicLogger {

  trait Simple extends super.Simple {
    override protected def maybeComponentName(): Option[String] = None
  }

  abstract class Actor extends super.Actor(None)

  abstract class TypedActor[T](ctx: ActorContext[T]) extends super.TypedActor[T](ctx, None)

  def getLogger[T](ctx: ActorContext[T]): LoggerImpl = getLogger(ctx, None)
}

/**
 * Extend this class to obtain a reference to a Simple or Actor Logger with the provided component name
 *
 * @param _componentName name of the component to initialize the logger
 */
class ServiceLogger(_componentName: String) extends BasicLogger {

  trait Simple extends super.Simple {
    override protected def maybeComponentName(): Option[String] = Some(_componentName)
  }

  abstract class Actor extends super.Actor(Some(_componentName))

  abstract class TypedActor[T](ctx: ActorContext[T]) extends super.TypedActor[T](ctx, Some(_componentName))

  def getLogger[T](ctx: ActorContext[T]): LoggerImpl = getLogger(ctx, Some(_componentName))

}

object ComponentLogger extends BasicLogger {
  def getLogger[T](ctx: ActorContext[T], componentName: String): Logger = getLogger(ctx, Some(componentName))
}

class BasicLogger {

  /**
   * Mix in this trait into your class to obtain a reference to a logger initialized with name of the component
   */
  trait Simple {
    protected def maybeComponentName(): Option[String]
    protected val log: Logger = new LoggerImpl(maybeComponentName(), None)
  }

  /**
   * Mix in this trait to create an Actor and obtain a reference to a logger initialized with name of the component and it's ActorPath
   *
   * @param maybeComponentName name of the component to initialize the logger
   */
  abstract class Actor private[logging] (maybeComponentName: Option[String]) extends akka.actor.Actor {
    def this(componentName: String) = this(Some(componentName))
    protected val log: Logger = new LoggerImpl(maybeComponentName, Some(actorPath(self)))
  }

  /**
   * Mix in this trait to create a TypedActor and obtain a reference to a logger initialized with name of the component and it's ActorPath
   *
   * @param maybeComponentName name of the component to initialize the logger
   */
  abstract class TypedActor[T] private[logging] (ctx: ActorContext[T], maybeComponentName: Option[String])
      extends Actor.MutableBehavior[T] {

    def this(ctx: ActorContext[T], componentName: String) = this(ctx, Some(componentName))
    protected lazy val log: Logger = getLogger(ctx, maybeComponentName)
  }

  private[logging] def getLogger[T](ctx: ActorContext[T], maybeComponentName: Option[String]) =
    new LoggerImpl(maybeComponentName, Some(actorPath(ctx.self.toUntyped)))

  private[logging] def actorPath(actorRef: ActorRef): String =
    ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}
