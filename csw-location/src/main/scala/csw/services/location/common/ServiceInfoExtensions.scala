package csw.services.location.common

import java.net.URI
import javax.jmdns.ServiceInfo

import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

object ServiceInfoExtensions {

  implicit class RichServiceInfo(val info: ServiceInfo) extends AnyVal {

    def locations: List[Location] = {
      val connection = Connection.parse(info.getName).get

      def getUri(uriStr: String): Option[URI] = connection match {
        case _: AkkaConnection =>
          val path = info.getPropertyString(LocationService.PathKey)
          if (path == null) None
          else getAkkaUri(uriStr, info.getPropertyString(LocationService.SystemKey))
        case _                 =>
          Some(new URI(uriStr))
      }

      val urls = info.getURLs(connection.connectionType.name).toList

      urls.flatMap(getUri).map { uri =>
        connection match {
          case conn: TcpConnection  =>
            ResolvedTcpLocation(conn, uri)
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

}
