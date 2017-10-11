package csw.services.location.commons

import akka.typed.ActorRef
import csw.messages.location.Connection.{AkkaConnection, HttpConnection}
import csw.services.location.models.{AkkaRegistration, HttpRegistration}

object RegistrationFactory {
  def akka(connection: AkkaConnection, actorRef: ActorRef[_]) =
    AkkaRegistration(connection, actorRef, null)

  def http(connection: HttpConnection, port: Int, path: String) = HttpRegistration(connection, port, path, null)
}
