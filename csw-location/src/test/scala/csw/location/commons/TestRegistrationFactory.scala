package csw.location.commons

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models.{AkkaRegistration, HttpRegistration, TcpRegistration}
import csw.params.core.models.Prefix
import csw.logging.commons.LogAdminActorFactory
import csw.logging.messages.LogControlMessages

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
