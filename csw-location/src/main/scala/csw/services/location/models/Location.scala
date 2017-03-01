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

  def fromServiceInfo(info: ServiceInfo): List[Location] = {
    val connection = Connection.parse(info.getName).get

    def getUri(uriStr: String): Option[URI] = connection match {
      case _: AkkaConnection =>
        val path = info.getPropertyString(LocationService.PathKey)
        if (path == null) None
        else getAkkaUri(uriStr, info.getPropertyString(LocationService.SystemKey))
      case _                 =>
        Some(new URI(uriStr))
    }

    info.getURLs(connection.connectionType.name).toList.flatMap(getUri).map { uri =>
      connection match {
        case conn: TcpConnection  =>
          ResolvedTcpLocation(conn, uri.getHost, uri.getPort)
        case conn: HttpConnection =>
          val path = info.getPropertyString(LocationService.PathKey)
          ResolvedHttpLocation(conn, uri, path)
        case conn: AkkaConnection =>
          val prefix = info.getPropertyString(LocationService.PrefixKey)
          ResolvedAkkaLocation(conn, uri, prefix)
      }
    }
  }

  private def getAkkaUri(uriStr: String, userInfo: String): Option[URI] = try {
    val uri = new URI(uriStr)
    Some(new URI("akka.tcp", userInfo, uri.getHost, uri.getPort, uri.getPath, uri.getQuery, uri.getFragment))
  } catch {
    case e: Exception =>
      // some issue with ipv6 addresses?
      println(s"Couldn't make URI from $uriStr and userInfo $userInfo")
      None
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
