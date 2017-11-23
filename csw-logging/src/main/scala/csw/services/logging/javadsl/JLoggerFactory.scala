package csw.services.logging.javadsl

import akka.actor
import akka.typed.javadsl.ActorContext
import csw.services.logging.internal.JLogger
import csw.services.logging.scaladsl.{Logger, LoggerFactory, LoggerImpl}

private[logging] abstract class JBaseLoggerFactory(maybeComponentName: Option[String]) {
  def getLogger[T](ctx: ActorContext[T], klass: Class[_]): ILogger = new JLogger(logger(Some(ctx.getSelf.toString)), klass)
  def getLogger(ctx: actor.ActorContext, klass: Class[_]): ILogger = new JLogger(logger(Some(ctx.self.toString())), klass)
  def getLogger(klass: Class[_]): ILogger                          = new JLogger(logger(None), klass)

  private def logger(maybeActorRef: Option[String]): Logger = new LoggerImpl(maybeComponentName, maybeActorRef)
}

class JLoggerFactory(componentName: String) extends JBaseLoggerFactory(Some(componentName)) {
  def asScala: LoggerFactory = new LoggerFactory(componentName)
}

object JGenericLoggerFactory extends JBaseLoggerFactory(None)
