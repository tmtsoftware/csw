package csw.services.logging.scaladsl

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import akka.typed
import akka.typed.scaladsl.adapter.TypedActorRefOps

class LoggerFactory(componentName: String) {

  def getLogger[T](actorRef: typed.ActorRef[T]): Logger = new LoggerImpl(Some(componentName), Some(actorPath(actorRef.toUntyped)))
  def getLogger(actorRef: ActorRef): Logger             = new LoggerImpl(Some(componentName), Some(actorPath(actorRef)))
  def getLogger: Logger                                 = new LoggerImpl(Some(componentName), None)

  private def actorPath(actorRef: ActorRef): String = ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}
