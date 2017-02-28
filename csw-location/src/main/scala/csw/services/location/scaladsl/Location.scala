package csw.services.location.scaladsl

import java.net.URI
import java.util.Optional

import akka.actor.ActorRef
import csw.services.location.scaladsl.Connection.{AkkaConnection, HttpConnection, TcpConnection}

import scala.compat.java8.OptionConverters.{RichOptionForJava8, RichOptionalGeneric}

sealed trait Location {
  def connection: Connection
}

final case class Unresolved(connection: Connection) extends Location
final case class Removed(connection: Connection) extends Location

sealed trait Resolved extends Location

final case class ResolvedAkkaLocation(connection: AkkaConnection, uri: URI, prefix: String = "", actorRef: Option[ActorRef] = None) extends Resolved {
  /**
    * Java constructor
    */
  def this(connection: AkkaConnection, uri: URI, prefix: String, actorRef: Optional[ActorRef]) = this(connection, uri, prefix, actorRef.asScala)

  /**
    * Java API to get actorRef
    * @return
    */
  def getActorRef: Optional[ActorRef] = actorRef.asJava
}

final case class ResolvedHttpLocation(connection: HttpConnection, uri: URI, path: String) extends Resolved

final case class ResolvedTcpLocation(connection: TcpConnection, host: String, port: Int) extends Resolved
