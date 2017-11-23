package csw.services.logging.scaladsl

import acyclic.skipped
import akka.actor
import akka.typed.scaladsl.ActorContext
import csw.services.logging.javadsl.JLoggerFactory

private[logging] abstract class BaseLoggerFactory(maybeComponentName: Option[String]) {
  def getLogger[T](ctx: ActorContext[T]): Logger = new LoggerImpl(maybeComponentName, Some(ctx.self.toString))
  def getLogger(ctx: actor.ActorContext): Logger = new LoggerImpl(maybeComponentName, Some(ctx.self.toString))
  def getLogger: Logger                          = new LoggerImpl(maybeComponentName, None)
}

class LoggerFactory(componentName: String) extends BaseLoggerFactory(Some(componentName)) {
  def asJava: JLoggerFactory = new JLoggerFactory(componentName)
}

object GenericLoggerFactory extends BaseLoggerFactory(None)
