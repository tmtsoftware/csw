package csw.services.location.common

import java.net.URI
import javax.jmdns.ServiceInfo

import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.util.control.NonFatal

object ServiceInfoExtensions {

  implicit class RichServiceInfo(val info: ServiceInfo) extends AnyVal {

    def locations: List[Location] = {
      val connection = Connection.parse(info.getName).get
      val urls = info.getURLs(connection.connectionType.name).toList
      urls.map(url => resolve(connection, url))
    }

    private def resolve(connection: Connection, url: String): Resolved = try {
      connection match {
        case conn: TcpConnection  =>
          ResolvedTcpLocation(conn, new URI(url))
        case conn: HttpConnection =>
          val path = info.getPropertyString(LocationService.PathKey)
          ResolvedHttpLocation(conn, new URI(url), path)
        case conn: AkkaConnection =>
          val prefix = info.getPropertyString(LocationService.PrefixKey)
          val path = info.getPropertyString(LocationService.PathKey)
          assert(path != null)
          val sysValue = info.getPropertyString(LocationService.SystemKey)
          val u = new URI(url)
          val uri = new URI("akka.tcp", sysValue, u.getHost, u.getPort, u.getPath, u.getQuery, u.getFragment)
          ResolvedAkkaLocation(conn, uri, prefix)
      }
    } catch {
      case NonFatal(ex) => throw new RuntimeException(s"error resolving location for connection=$connection and url=$url")
    }

  }

}
