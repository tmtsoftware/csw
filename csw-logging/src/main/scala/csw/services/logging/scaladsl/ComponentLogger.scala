package csw.services.logging.scaladsl

import akka.actor.ActorPath
import akka.serialization.Serialization
/**
 * Extend this object to obtain a reference to a Simple or Actor Logger without a component name
 */
object GenericLogger extends BasicLogger(None)

/**
 * Extend this class to obtain a reference to a Simple or Actor Logger with the provided component name
 * @param componentName name of the component to initialize the logger
 */
class ComponentLogger(componentName: String) extends BasicLogger(Some(componentName))

class BasicLogger(componentName: Option[String]) {

  /**
   * Mix in this trait into your class to obtain a reference to a logger initialized with name of the component
   */
  trait Simple {
    protected val log: Logger = new LoggerImpl(componentName, None)
  }

  /**
   * Mix in this trait to create an Actor and obtain a reference to a logger initialized with name of the component and it's ActorPath
   */
  abstract class Actor extends akka.actor.Actor {
    private val actorName     = Some(ActorPath.fromString(Serialization.serializedActorPath(self)).toString)
    protected val log: Logger = new LoggerImpl(componentName, actorName)
  }

  import akka.typed.scaladsl.{Actor, ActorContext}
  import akka.typed.scaladsl.adapter._
  abstract class TypedActor[T](ctx: ActorContext[T]) extends Actor.MutableBehavior[T] {
    private val actorName     = Some(ActorPath.fromString(Serialization.serializedActorPath(ctx.self.toUntyped)).toString)
    protected val log: Logger = new LoggerImpl(componentName, actorName)
  }
}
