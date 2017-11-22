package csw.services.logging.scaladsl

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import akka.typed.scaladsl.adapter.TypedActorRefOps
import akka.typed.scaladsl.{Actor, ActorContext}

private[logging] abstract class AbstractLogger {

  private[logging] trait Simple {
    protected def maybeName: Option[String]
    protected val log: Logger = new LoggerImpl(maybeName, None)
  }

  private[logging] abstract class Actor(maybeName: Option[String]) extends akka.actor.Actor {
    protected val log: Logger = new LoggerImpl(maybeName, Some(actorPath(self)))
  }

  private[logging] abstract class MutableActor[T](ctx: ActorContext[T], maybeName: Option[String])
      extends Actor.MutableBehavior[T] {
    protected lazy val log: Logger = new LoggerImpl(maybeName, Some(actorPath(ctx.self.toUntyped)))
  }

  private[logging] def immutable[T](ctx: ActorContext[T], maybeName: Option[String]): Logger =
    new LoggerImpl(maybeName, Some(actorPath(ctx.self.toUntyped)))

  private def actorPath(actorRef: ActorRef): String =
    ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}

private[logging] class StaticLogger(_maybeName: Option[String]) extends AbstractLogger {

  trait Simple extends super.Simple {
    override protected def maybeName: Option[String] = _maybeName
  }

  abstract class Actor extends super.Actor(_maybeName)

  abstract class MutableActor[T](ctx: ActorContext[T]) extends super.MutableActor[T](ctx, _maybeName)

  def immutable[T](ctx: ActorContext[T]): Logger = immutable(ctx, _maybeName)
}

/**
 * Extend this class to access Loggers(Simple, Actor, MutableActor, immutable) with the provided component name
 *
 * @param name name of the component to initialize the logger
 */
class LibraryLogger(name: String) extends StaticLogger(Some(name))

/**
 * Extend this object to access Loggers(Simple, Actor, MutableActor, immutable) without a component name
 */
object GenericLogger extends StaticLogger(None)

/**
 * Extend this object to access Loggers(Simple, Actor, MutableActor, immutable) which has componentName as constructor
 * dependency. It is mainly used when componentName is not available before-hand.
 */
object FrameworkLogger extends AbstractLogger {

  trait Simple extends super.Simple {
    protected def componentName(): String
    override protected def maybeName: Option[String] = Some(componentName())
  }

  abstract class Actor(componentName: String) extends super.Actor(Some(componentName))

  abstract class MutableActor[T](ctx: ActorContext[T], componentName: String)
      extends super.MutableActor[T](ctx, Some(componentName))

  def immutable[T](ctx: ActorContext[T], componentName: String): Logger = immutable(ctx, Some(componentName))
}
