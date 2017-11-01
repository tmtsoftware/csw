package csw.services.location.commons

import akka.typed.ActorRef
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models.{AkkaRegistration, HttpRegistration, TcpRegistration}

object RegistrationFactory {
  def akka(connection: AkkaConnection, actorRef: ActorRef[_]) =
    AkkaRegistration(connection, Some("nfiraos.ncc.trombone"), actorRef, null)

  def akka(connection: AkkaConnection, prefix: String, actorRef: ActorRef[_]) =
    AkkaRegistration(connection, Some(prefix), actorRef, null)

  def http(connection: HttpConnection, port: Int, path: String) = HttpRegistration(connection, port, path, null)
  def tcp(connection: TcpConnection, port: Int)                 = TcpRegistration(connection, port, null)
}
