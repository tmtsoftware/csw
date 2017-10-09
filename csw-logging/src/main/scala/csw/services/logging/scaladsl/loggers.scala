package csw.services.logging.scaladsl

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import akka.typed.scaladsl.adapter.TypedActorRefOps
import akka.typed.scaladsl.{Actor, ActorContext}

private[logging] class BasicLogger {

  private[logging] trait Simple {
    protected def maybeComponentName(): Option[String]
    protected val log: Logger = new LoggerImpl(maybeComponentName(), None)
  }

  private[logging] abstract class Actor(maybeComponentName: Option[String]) extends akka.actor.Actor {
    protected val log: Logger = new LoggerImpl(maybeComponentName, Some(actorPath(self)))
  }

  private[logging] abstract class MutableActor[T](ctx: ActorContext[T], maybeComponentName: Option[String])
      extends Actor.MutableBehavior[T] {
    protected lazy val log: Logger = new LoggerImpl(maybeComponentName, Some(actorPath(ctx.self.toUntyped)))
  }

  private[logging] def immutable[T](ctx: ActorContext[T], maybeComponentName: Option[String]) =
    new LoggerImpl(maybeComponentName, Some(actorPath(ctx.self.toUntyped)))

  private def actorPath(actorRef: ActorRef): String =
    ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}

private[logging] class BasicServiceAndGenericLogger(_maybeComponentName: Option[String]) extends BasicLogger {

  trait Simple extends super.Simple {
    override protected def maybeComponentName(): Option[String] = _maybeComponentName
  }

  abstract class Actor extends super.Actor(_maybeComponentName)

  abstract class MutableActor[T](ctx: ActorContext[T]) extends super.MutableActor[T](ctx, _maybeComponentName)

  def immutable[T](ctx: ActorContext[T]): LoggerImpl = immutable(ctx, _maybeComponentName)
}

/**
 * Extend this class to obtain a reference to a Simple or Actor Logger with the provided component name
 *
 * @param _componentName name of the component to initialize the logger
 */
class ServiceLogger(_componentName: String) extends BasicServiceAndGenericLogger(Some(_componentName))

/**
 * Extend this object to obtain a reference to a Simple or Actor Logger without a component name
 */
object GenericLogger extends BasicServiceAndGenericLogger(None)

object ComponentLogger extends BasicLogger {

  trait Simple extends super.Simple {
    protected def componentName(): String
    override protected def maybeComponentName(): Option[String] = Some(componentName())
  }

  abstract class Actor(componentName: String) extends super.Actor(Some(componentName))

  abstract class MutableActor[T](ctx: ActorContext[T], componentName: String)
      extends super.MutableActor[T](ctx, Some(componentName))

  def immutable[T](ctx: ActorContext[T], componentName: String): Logger = immutable(ctx, Some(componentName))
}
