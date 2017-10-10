package csw.services.logging.javadsl
import akka.actor.{AbstractActor, ActorPath, ActorRef}
import akka.serialization.Serialization
import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.adapter._
import csw.services.logging.internal.JLogger
import csw.services.logging.scaladsl.LoggerImpl

private[logging] object JBasicLogger {
  def getLogger(maybeComponentName: Option[String], maybeActorRef: Option[ActorRef], klass: Class[_]): ILogger = {
    val log = new LoggerImpl(maybeComponentName, maybeActorRef.map(actorPath))
    new JLogger(log, klass)
  }

  private def actorPath(actorRef: ActorRef): String =
    ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}

/**
 * Implement this trait to obtain a reference to a generic logger which is not initialized with component name
 */
trait JGenericLogger {
  def getLogger: ILogger = JBasicLogger.getLogger(None, None, getClass)
}

/**
 * Extend this class to create an Actor and obtain a reference to a generic logger which is initialized with Actor path but no component name
 */
abstract class JGenericLoggerActor extends AbstractActor {
  def getLogger: ILogger = JBasicLogger.getLogger(None, Some(getSelf()), getClass)
}

abstract class JGenericLoggerMutableActor[T](ctx: ActorContext[T]) extends akka.typed.javadsl.Actor.MutableBehavior[T] {
  def getLogger: ILogger = JBasicLogger.getLogger(None, Some(ctx.getSelf.toUntyped), getClass)
}

final class JGenericLoggerImmutable {
  def getLogger[T](ctx: ActorContext[T], klass: Class[_]): ILogger =
    JBasicLogger.getLogger(None, Some(ctx.getSelf.toUntyped), klass)
}

/**
 * Implement this trait to provide a component name and obtain a reference to a logger initialized with the name of the component
 */
trait JComponentLogger {
  protected def componentName: String
  def getLogger: ILogger = JBasicLogger.getLogger(Some(componentName), None, getClass)
}

/**
 * Extend this class to create an Actor and provide a component name to obtain a reference to a logger initialized with the name of the component and it's ActorPath
 */
abstract class JComponentLoggerActor extends AbstractActor {
  protected def componentName: String
  def getLogger: ILogger = JBasicLogger.getLogger(Some(componentName), Some(getSelf()), getClass)
}

abstract class JComponentLoggerMutableActor[T](ctx: ActorContext[T])
    extends akka.typed.javadsl.Actor.MutableBehavior[T] {
  protected def componentName: String
  def getLogger: ILogger = JBasicLogger.getLogger(Some(componentName), Some(ctx.getSelf.toUntyped), getClass)
}

final class JComponentLoggerImmutable {
  def getLogger[T](ctx: ActorContext[T], componentName: String, klass: Class[_]): ILogger =
    JBasicLogger.getLogger(Some(componentName), Some(ctx.getSelf.toUntyped), klass)
}
