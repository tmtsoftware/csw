package csw.services.logging.scaladsl

import akka.actor.ActorPath
import akka.serialization.Serialization

object GenericLogger extends BasicLogger(None)

class ComponentLogger(componentName: String) extends BasicLogger(Some(componentName))

class BasicLogger(componentName: Option[String]) {
  trait Simple {
    protected val log: Logger = new LoggerImpl(componentName, None)
  }
  trait Actor extends akka.actor.Actor {
    private val actorName     = Some(ActorPath.fromString(Serialization.serializedActorPath(self)).toString)
    protected val log: Logger = new LoggerImpl(componentName, actorName)
  }
}
