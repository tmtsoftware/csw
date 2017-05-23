package csw.services.logging.scaladsl

import akka.actor.ActorPath
import akka.serialization.Serialization

class ComponentLogger(componentName: Option[String]) {
  trait Simple {
    val log = new Logger(componentName, None)
  }
  trait Actor extends akka.actor.Actor {
    val actorName = Some(ActorPath.fromString(Serialization.serializedActorPath(self)).toString)
    val log       = new Logger(componentName, actorName)
  }
}
