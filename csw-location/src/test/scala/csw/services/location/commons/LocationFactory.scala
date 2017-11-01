package csw.services.location.commons

import java.net.URI

import akka.typed.ActorRef
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.messages.location.{AkkaLocation, HttpLocation, TcpLocation}

object LocationFactory {
  def akka(connection: AkkaConnection, uri: URI, actorRef: ActorRef[_]) =
    AkkaLocation(connection, Some("nfiraos.ncc.trombone"), uri, actorRef, null)
  def http(connection: HttpConnection, uri: URI) = HttpLocation(connection, uri, null)
  def tcp(connection: TcpConnection, uri: URI)   = TcpLocation(connection, uri, null)
}
