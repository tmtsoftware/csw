package csw.location.api.extensions

import java.net.URI

import akka.actor.typed.{ActorRef, ActorRefResolver, ActorSystem}

object URIExtension {
  implicit class RichURI(val uri: URI) {
    def toActorRef(implicit system: ActorSystem[_]): ActorRef[Nothing] = ActorRefResolver(system).resolveActorRef(uri.toString)
  }
}
