package csw.services.logging.scaladsl

import akka.actor
import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorRefOps
import csw.services.logging.javadsl.JLoggerFactory

private[logging] abstract class BaseLoggerFactory(maybeComponentName: Option[String]) {
  def getLogger[T](ctx: ActorContext[T]): Logger = new LoggerImpl(maybeComponentName, Some(actorPath(ctx.self.toUntyped)))
  def getLogger(ctx: actor.ActorContext): Logger = new LoggerImpl(maybeComponentName, Some(actorPath(ctx.self)))
  def getLogger: Logger                          = new LoggerImpl(maybeComponentName, None)

  private def actorPath(actorRef: ActorRef): String = ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}

class LoggerFactory(componentName: String) extends BaseLoggerFactory(Some(componentName)) {
  def asJava: JLoggerFactory = new JLoggerFactory(componentName)
}

object GenericLoggerFactory extends BaseLoggerFactory(None)
