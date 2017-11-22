package csw.services.logging.javadsl

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import akka.typed
import akka.typed.scaladsl.adapter.TypedActorRefOps
import csw.services.logging.internal.JLogger
import csw.services.logging.scaladsl.LoggerImpl

class JLoggerFactory(componentName: String) {

  def getLogger[T](actorRef: typed.ActorRef[T], klass: Class[_]): ILogger = {
    val logger = new LoggerImpl(Some(componentName), Some(actorPath(actorRef.toUntyped)))
    new JLogger(logger, klass)
  }

  def getLogger(actorRef: ActorRef, klass: Class[_]): ILogger = {
    val logger = new LoggerImpl(Some(componentName), Some(actorPath(actorRef)))
    new JLogger(logger, klass)
  }

  def getLogger(klass: Class[_]): ILogger = {
    val logger = new LoggerImpl(Some(componentName), None)
    new JLogger(logger, klass)
  }

  private def actorPath(actorRef: ActorRef): String = ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}
