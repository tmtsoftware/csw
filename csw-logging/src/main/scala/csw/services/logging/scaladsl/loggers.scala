package csw.services.logging.scaladsl

import akka.actor.ActorPath
import akka.serialization.Serialization
import akka.typed.scaladsl.{Actor, ActorContext}

/**
 * Extend this object to obtain a reference to a Simple or Actor Logger without a component name
 */
object GenericLogger extends BasicLogger {
  trait Simple extends super.Simple {
    override protected val componentName: String = ""
  }
  abstract class Actor                               extends super.Actor("")
  abstract class TypedActor[T](ctx: ActorContext[T]) extends super.TypedActor[T](ctx, "")
}

/**
 * Extend this class to obtain a reference to a Simple or Actor Logger with the provided component name
 * @param _componentName name of the component to initialize the logger
 */
class ComponentLogger(_componentName: String) extends BasicLogger {
  trait Simple extends super.Simple {
    override protected val componentName: String = _componentName
  }
  abstract class Actor                               extends super.Actor(_componentName)
  abstract class TypedActor[T](ctx: ActorContext[T]) extends super.TypedActor[T](ctx, _componentName)
}

object ComponentLogger extends BasicLogger

class BasicLogger {

  /**
   * Mix in this trait into your class to obtain a reference to a logger initialized with name of the component
   */
  trait Simple {
    protected val componentName: String
    private lazy val maybeComponentName: Option[String] =
      if (!componentName.isEmpty) Some(componentName)
      else None

    protected lazy val log: Logger = new LoggerImpl(maybeComponentName, None)
  }

  /**
   * Mix in this trait to create an Actor and obtain a reference to a logger initialized with name of the component and it's ActorPath
   * @param componentName name of the component to initialize the logger
   */
  abstract class Actor(componentName: String) extends akka.actor.Actor {
    private lazy val maybeComponentName: Option[String] =
      if (!componentName.isEmpty) Some(componentName)
      else None

    private lazy val actorName     = Some(ActorPath.fromString(Serialization.serializedActorPath(self)).toString)
    protected lazy val log: Logger = new LoggerImpl(maybeComponentName, actorName)
  }

  import akka.typed.scaladsl.adapter._
  import akka.typed.scaladsl.ActorContext

  /**
   * Mix in this trait to create a TypedActor and obtain a reference to a logger initialized with name of the component and it's ActorPath
   * @param componentName name of the component to initialize the logger
   */
  abstract class TypedActor[T](ctx: ActorContext[T], componentName: String) extends Actor.MutableBehavior[T] {
    private lazy val maybeComponentName: Option[String] =
      if (!componentName.isEmpty) Some(componentName)
      else None

    private lazy val actorName = Some(
      ActorPath.fromString(Serialization.serializedActorPath(ctx.self.toUntyped)).toString
    )
    protected lazy val log: Logger = new LoggerImpl(maybeComponentName, actorName)
  }
}
