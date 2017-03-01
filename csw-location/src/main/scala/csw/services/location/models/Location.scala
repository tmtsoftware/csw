package csw.services.location.models

import java.net.URI
import java.util.Optional
import javax.jmdns.ServiceInfo

import akka.actor.ActorRef
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.scaladsl.LocationService

import scala.compat.java8.OptionConverters.{RichOptionForJava8, RichOptionalGeneric}

sealed trait Location {
  def connection: Connection
}

object Location {

  def fromServiceInfo(serviceInfo: ServiceInfo): List[Location] = {
    val connection = Connection.parse(serviceInfo.getName).get

    def getUri(uriStr: String): Option[URI] = connection match {
      case _: AkkaConnection => ???
      case _                 =>
        Some(new URI(uriStr))
    }

    serviceInfo.getURLs(connection.connectionType.name).toList.flatMap(getUri).map { uri =>
      connection match {
        case conn: TcpConnection  =>
          ResolvedTcpLocation(conn, uri.getHost, uri.getPort)
        case conn: HttpConnection =>
          val path = serviceInfo.getPropertyString(LocationService.PathKey)
          ResolvedHttpLocation(conn, uri, path)
        case conn: AkkaConnection => ???
      }
    }
  }
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
    *
    * @return
    */
  def getActorRef: Optional[ActorRef] = actorRef.asJava
}

final case class ResolvedHttpLocation(connection: HttpConnection, uri: URI, path: String) extends Resolved

final case class ResolvedTcpLocation(connection: TcpConnection, host: String, port: Int) extends Resolved
