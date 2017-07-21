package csw.services.logging.javadsl
import java.util.Optional

import akka.actor.{AbstractActor, ActorPath}
import akka.serialization.Serialization
import akka.typed.javadsl.ActorContext
import csw.services.logging.internal.JLogger
import csw.services.logging.scaladsl.LoggerImpl

import scala.compat.java8.OptionConverters.RichOptionalGeneric

/**
 * Implement this trait to obtain a reference to a generic logger which is not initialized with component name
 */
trait JGenericLogger extends JBasicLogger {
  override protected def maybeComponentName: Optional[String] = Optional.empty()
}

/**
 * Extend this class to create an Actor and obtain a reference to a generic logger which is initialized with Actor path but no component name
 */
abstract class JGenericLoggerActor extends JBasicLoggerActor {
  override protected def maybeComponentName: Optional[String] = Optional.empty()
}

abstract class JGenericLoggerTypedActor[T](actorContext: ActorContext[T]) extends JBasicLoggerTypedActor[T](actorContext) {
  override protected def maybeComponentName: Optional[String] = Optional.empty()
}

/**
 * Implement this trait to provide a component name and obtain a reference to a logger initialized with the name of the component
 */
trait JComponentLogger extends JBasicLogger {
  protected def componentName: String
  override protected def maybeComponentName: Optional[String] = Optional.of(componentName)
}

/**
 * Extend this class to create an Actor and provide a component name to obtain a reference to a logger initialized with the name of the component and it's ActorPath
 */
abstract class JComponentLoggerActor extends JBasicLoggerActor {
  protected def componentName: String
  override protected def maybeComponentName: Optional[String] = Optional.of(componentName)
}

abstract class JComponentLoggerTypedActor[T](actorContext: ActorContext[T]) extends JBasicLoggerTypedActor(actorContext) {
  protected def componentName: String
  override protected def maybeComponentName: Optional[String] = Optional.of(componentName)
}

/**
 * Implement this trait to obtain a reference to a logger initialized with name of the component
 */
trait JBasicLogger {
  protected def maybeComponentName: Optional[String]
  protected def getLogger: ILogger = {
    val log = new LoggerImpl(maybeComponentName.asScala, None)
    new JLogger(log, getClass)
  }
}

/**
 * Extend this class to create an Actor and obtain a reference to a logger initialized with name of the component and it's ActorPath
 */
abstract class JBasicLoggerActor extends AbstractActor {
  protected def maybeComponentName: Optional[String]
  protected def getLogger: ILogger = {
    val actorName = ActorPath.fromString(Serialization.serializedActorPath(getSelf)).toString
    val log       = new LoggerImpl(maybeComponentName.asScala, Some(actorName))
    new JLogger(log, getClass)
  }
}

import akka.typed.scaladsl.adapter._
abstract class JBasicLoggerTypedActor[T](actorContext: ActorContext[T]) extends akka.typed.javadsl.Actor.MutableBehavior[T] {
  protected def maybeComponentName: Optional[String]
  protected def getLogger: ILogger = {
    val actorName = ActorPath.fromString(Serialization.serializedActorPath(actorContext.getSelf.toUntyped)).toString
    val log       = new LoggerImpl(maybeComponentName.asScala, Some(actorName))
    new JLogger(log, getClass)
  }
}
