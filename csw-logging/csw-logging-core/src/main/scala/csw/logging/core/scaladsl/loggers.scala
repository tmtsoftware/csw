package csw.logging.core.scaladsl

import acyclic.skipped
import akka.actor
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import csw.logging.core.internal.LoggerImpl
import csw.logging.core.javadsl.JLoggerFactory

abstract class BaseLoggerFactory private[logging] (maybeComponentName: Option[String]) {
  def getLogger[T](ctx: ActorContext[T]): Logger = new LoggerImpl(maybeComponentName, Some(actorPath(ctx.self.toUntyped)))
  def getLogger(ctx: actor.ActorContext): Logger = new LoggerImpl(maybeComponentName, Some(actorPath(ctx.self)))
  def getLogger: Logger                          = new LoggerImpl(maybeComponentName, None)

  private def actorPath(actorRef: ActorRef): String = ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}

/**
 * When using the `LoggerFactory`, log statements will have `@componentName` tag with provided `componentName`
 *
 * @param componentName to appear in log statements
 */
class LoggerFactory(componentName: String) extends BaseLoggerFactory(Some(componentName)) {

  /**
   * Returns the java API for this instance of LoggerFactory
   */
  def asJava: JLoggerFactory = new JLoggerFactory(componentName)
}

/**
 * When using the `GenericLoggerFactory`, log statements will not have `@componentName` tag
 */
object GenericLoggerFactory extends BaseLoggerFactory(None)
