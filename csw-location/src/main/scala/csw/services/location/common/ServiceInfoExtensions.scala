package csw.services.location.common

import java.net.URI
import javax.jmdns.ServiceInfo

import akka.actor.{ActorPath, ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionKey}
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.util.control.NonFatal

object ServiceInfoExtensions {

  implicit class RichServiceInfo(val info: ServiceInfo) extends AnyVal {

    def locations(implicit actorSystem: ActorSystem): List[Location] = {
      val connection = Connection.parse(info.getName).get
      val urls = info.getURLs(connection.connectionType.name).toList
      urls.map(url => resolve(connection, url))
    }

    private def resolve(connection: Connection, url: String)(implicit actorSystem: ActorSystem): Resolved = try {
      connection match {
        case conn: TcpConnection  =>
          ResolvedTcpLocation(conn, new URI(url))
        case conn: HttpConnection =>
          val path = info.getPropertyString(LocationService.PathKey)
          ResolvedHttpLocation(conn, new URI(url), path)
        case conn: AkkaConnection =>
          val prefix = info.getPropertyString(LocationService.PrefixKey)
          val actorPathString = info.getPropertyString(LocationService.ActorPathKey)
          val actorRef = RemoteAddressExtension(actorSystem).resolveActorRef(actorPathString)
          val uri = new URI(ActorPath.fromString(actorPathString).toString)
          ResolvedAkkaLocation(conn, uri, prefix, Some(actorRef))
      }
    } catch {
      case NonFatal(ex) => throw new RuntimeException(s"error resolving location for connection=$connection and url=$url")
    }

  }

  private class RemoteAddressExtensionImpl(system: ExtendedActorSystem) extends Extension {
    def resolveActorRef(actorPathString: String): ActorRef = system.provider.resolveActorRef(actorPathString)
  }

  private object RemoteAddressExtension extends ExtensionKey[RemoteAddressExtensionImpl]

}
