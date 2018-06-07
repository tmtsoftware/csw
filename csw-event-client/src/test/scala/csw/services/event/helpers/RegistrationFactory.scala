package csw.services.event.helpers

import akka.actor.typed.ActorRef
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models.{AkkaRegistration, HttpRegistration, TcpRegistration}

object RegistrationFactory {
  def akka(connection: AkkaConnection, actorRef: ActorRef[_]): AkkaRegistration =
    AkkaRegistration(connection, Some("nfiraos.ncc.trombone"), actorRef, null)

  def akka(connection: AkkaConnection, prefix: String, actorRef: ActorRef[_]): AkkaRegistration =
    AkkaRegistration(connection, Some(prefix), actorRef, null)

  def http(connection: HttpConnection, port: Int, path: String): HttpRegistration =
    HttpRegistration(connection, port, path, null)

  def tcp(connection: TcpConnection, port: Int): TcpRegistration = TcpRegistration(connection, port, null)
}
