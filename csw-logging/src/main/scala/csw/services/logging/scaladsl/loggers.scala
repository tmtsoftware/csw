package csw.services.logging.scaladsl

import acyclic.skipped
import akka.actor
import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorRefOps
import csw.services.logging.internal.LoggerImpl
import csw.services.logging.javadsl.JLoggerFactory

private[logging] abstract class BaseLoggerFactory(maybeComponentName: Option[String]) {
  def getLogger[T](ctx: ActorContext[T]): Logger = new LoggerImpl(maybeComponentName, Some(actorPath(ctx.self.toUntyped)))
  def getLogger(ctx: actor.ActorContext): Logger = new LoggerImpl(maybeComponentName, Some(actorPath(ctx.self)))
  def getLogger: Logger                          = new LoggerImpl(maybeComponentName, None)

  private def actorPath(actorRef: ActorRef): String = ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}

/**
 * When using the `LoggerFactory`, log statements will have `@componentName` tag with provided `componentName`
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
