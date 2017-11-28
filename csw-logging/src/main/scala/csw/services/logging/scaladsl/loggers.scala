package csw.services.logging.scaladsl

import acyclic.skipped
import akka.actor
import akka.typed.scaladsl.ActorContext
import csw.services.logging.javadsl.JLoggerFactory

private[logging] abstract class BaseLoggerFactory(maybeComponentName: Option[String]) {
  def getLogger[T](ctx: ActorContext[T]): Logger = new LoggerImpl(maybeComponentName, Some(ctx.self.path.toString))
  def getLogger(ctx: actor.ActorContext): Logger = new LoggerImpl(maybeComponentName, Some(ctx.self.path.toString))
  def getLogger: Logger                          = new LoggerImpl(maybeComponentName, None)
}

/**
 * When using the `LoggerFactory`, log statements will have `@componentName` tag with provided `componentName`
 */
class LoggerFactory(componentName: String) extends BaseLoggerFactory(Some(componentName)) {
  def asJava: JLoggerFactory = new JLoggerFactory(componentName)
}

/**
 * When using the `GenericLoggerFactory`, log statements will not have `@componentName` tag
 */
object GenericLoggerFactory extends BaseLoggerFactory(None)
