package csw.services.location.commons

import java.net.URI

import akka.typed.ActorRef
import csw.messages.location.Connection.{AkkaConnection, HttpConnection}
import csw.messages.location.{AkkaLocation, HttpLocation}

object LocationFactory {
  def akka(connection: AkkaConnection, uri: URI, actorRef: ActorRef[_]) =
    AkkaLocation(connection, uri, actorRef, null)

  def http(connection: HttpConnection, uri: URI) = HttpLocation(connection, uri, null)
}
