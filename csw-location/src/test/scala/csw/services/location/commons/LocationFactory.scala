package csw.services.location.commons

import java.net.URI

import akka.actor.typed.ActorRef
import csw.services.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.api.models.{AkkaLocation, HttpLocation, TcpLocation}
import csw.params.core.models.Prefix

object LocationFactory {
  def akka(connection: AkkaConnection, uri: URI, actorRef: ActorRef[_], logAdminRef: ActorRef[_] = null) =
    AkkaLocation(connection, Prefix("nfiraos.ncc.trombone"), uri, actorRef, logAdminRef)
  def http(connection: HttpConnection, uri: URI, logAdminRef: ActorRef[_] = null) = HttpLocation(connection, uri, logAdminRef)
  def tcp(connection: TcpConnection, uri: URI, logAdminRef: ActorRef[_] = null)   = TcpLocation(connection, uri, logAdminRef)
}
