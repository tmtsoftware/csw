package csw.services.location.commons

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.api.models.{AkkaRegistration, HttpRegistration, TcpRegistration}
import csw.messages.params.models.Prefix
import csw.services.logging.commons.LogAdminActorFactory
import csw.services.logging.messages.LogControlMessages

class TestRegistrationFactory(implicit actorSystem: ActorSystem) {

  lazy val logAdminActorRef: ActorRef[LogControlMessages] = LogAdminActorFactory.make(actorSystem)

  def akka(connection: AkkaConnection, actorRef: ActorRef[_]): AkkaRegistration =
    AkkaRegistration(connection, Prefix("nfiraos.ncc.trombone"), actorRef, logAdminActorRef)

  def akka(connection: AkkaConnection, prefix: Prefix, actorRef: ActorRef[_]): AkkaRegistration =
    AkkaRegistration(connection, prefix, actorRef, logAdminActorRef)

  def http(connection: HttpConnection, port: Int, path: String): HttpRegistration =
    HttpRegistration(connection, port, path, logAdminActorRef)

  def tcp(connection: TcpConnection, port: Int): TcpRegistration =
    TcpRegistration(connection, port, logAdminActorRef)
}
