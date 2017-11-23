package csw.services.logging.javadsl

import acyclic.skipped
import akka.actor
import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorRefOps
import csw.services.logging.internal.JLogger
import csw.services.logging.scaladsl.{Logger, LoggerFactory, LoggerImpl}

private[logging] abstract class JBaseLoggerFactory(maybeComponentName: Option[String]) {
  def getLogger[T](ctx: ActorContext[T], klass: Class[_]): ILogger = {
    val logger: Logger = new LoggerImpl(maybeComponentName, Some(actorPath(ctx.getSelf.toUntyped)))
    new JLogger(logger, klass)
  }

  def getLogger(ctx: actor.ActorContext, klass: Class[_]): ILogger = {
    val logger: Logger = new LoggerImpl(maybeComponentName, Some(actorPath(ctx.self)))
    new JLogger(logger, klass)
  }

  def getLogger(klass: Class[_]): ILogger = {
    val logger: Logger = new LoggerImpl(maybeComponentName, None)
    new JLogger(logger, klass)
  }

  private def actorPath(actorRef: ActorRef): String = ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}

class JLoggerFactory(componentName: String) extends JBaseLoggerFactory(Some(componentName)) {
  def asScala: LoggerFactory = new LoggerFactory(componentName)
}

object JGenericLoggerFactory extends JBaseLoggerFactory(None)
