package csw.services.location.scaladsl.models

import java.net.URI
import java.util.Optional

import akka.actor.ActorRef
import csw.services.location.scaladsl.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

import scala.compat.java8.OptionConverters.RichOptionForJava8

sealed trait Location {
  def connection: Connection
}

final case class Unresolved(connection: Connection) extends Location

final case class Removed(connection: Connection) extends Location

sealed trait Resolved extends Location {
  def uri: URI
}

final case class ResolvedAkkaLocation(connection: AkkaConnection, uri: URI, prefix: String = "", actorRef: Option[ActorRef] = None) extends Resolved {
  /**
    * Java API to get actorRef
    *
    * @return
    */
  def getActorRef: Optional[ActorRef] = actorRef.asJava
}

final case class ResolvedHttpLocation(connection: HttpConnection, uri: URI, path: String) extends Resolved

final case class ResolvedTcpLocation(connection: TcpConnection, uri: URI) extends Resolved
