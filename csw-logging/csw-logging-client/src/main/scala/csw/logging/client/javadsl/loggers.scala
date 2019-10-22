package csw.logging.client.javadsl

import akka.actor
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import akka.actor.{typed, ActorPath, ActorRef}
import akka.serialization.Serialization
import csw.logging.api.javadsl.ILogger
import csw.logging.api.scaladsl.Logger
import csw.logging.client.internal.{JLoggerImpl, LoggerImpl}
import csw.logging.client.scaladsl.LoggerFactory

abstract class JBaseLoggerFactory private[logging] (maybeComponentName: Option[String]) {
  def getLogger[T](ctx: ActorContext[T], klass: Class[_]): ILogger = new JLoggerImpl(logger(Some(actorPath(ctx.getSelf))), klass)
  def getLogger(ctx: actor.ActorContext, klass: Class[_]): ILogger = new JLoggerImpl(logger(Some(actorPath(ctx.self))), klass)
  def getLogger(klass: Class[_]): ILogger                          = new JLoggerImpl(logger(None), klass)

  private def logger(maybeActorRef: Option[String]): Logger  = new LoggerImpl(maybeComponentName, maybeActorRef)
  private def actorPath(actorRef: ActorRef): String          = ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
  private def actorPath(actorRef: typed.ActorRef[_]): String = actorPath(actorRef.toClassic)
}

/**
 * When using the `JLoggerFactory`, log statements will have `@componentName` tag with provided `componentName`
 *
 * @param componentName to appear in log statements
 */
class JLoggerFactory(componentName: String) extends JBaseLoggerFactory(Some(componentName)) {

  /**
   * Returns the scala API for this instance of JLoggerFactory
   */
  def asScala: LoggerFactory = new LoggerFactory(componentName)
}

/**
 * When using the `JGenericLoggerFactory`, log statements will not have `@componentName` tag
 */
object JGenericLoggerFactory extends JBaseLoggerFactory(None)
