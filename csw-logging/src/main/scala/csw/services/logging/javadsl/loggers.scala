package csw.services.logging.javadsl

import acyclic.skipped
import akka.actor
import akka.typed.javadsl.ActorContext
import csw.services.logging.internal.JLogger
import csw.services.logging.scaladsl.{Logger, LoggerFactory, LoggerImpl}

private[logging] abstract class JBaseLoggerFactory(maybeComponentName: Option[String]) {
  def getLogger[T](ctx: ActorContext[T], klass: Class[_]): ILogger = new JLogger(logger(Some(ctx.getSelf.path.toString)), klass)
  def getLogger(ctx: actor.ActorContext, klass: Class[_]): ILogger = new JLogger(logger(Some(ctx.self.path.toString)), klass)
  def getLogger(klass: Class[_]): ILogger                          = new JLogger(logger(None), klass)

  private def logger(maybeActorRef: Option[String]): Logger = new LoggerImpl(maybeComponentName, maybeActorRef)
}

/**
 * When using the `JLoggerFactory`, log statements will have `@componentName` tag with provided `componentName`
 */
class JLoggerFactory(componentName: String) extends JBaseLoggerFactory(Some(componentName)) {
  def asScala: LoggerFactory = new LoggerFactory(componentName)
}

/**
 * When using the `JGenericLoggerFactory`, log statements will not have `@componentName` tag
 */
object JGenericLoggerFactory extends JBaseLoggerFactory(None)
