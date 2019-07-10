package csw.location.api.extensions

import java.net.URI

import akka.actor.typed.{ActorRef, ActorRefResolver, ActorSystem}

object ActorExtension {
  implicit class RichActor(actorRef: ActorRef[_]) {
    def toURI(implicit system: ActorSystem[_]): URI = new URI(ActorRefResolver(system).toSerializationFormat(actorRef))
  }
}
