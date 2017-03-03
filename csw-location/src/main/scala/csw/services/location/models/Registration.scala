package csw.services.location.models

import java.net.URI
import javax.jmdns.ServiceInfo

import akka.actor.ActorRef
import akka.serialization.Serialization
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.scaladsl.LocationService
import collection.JavaConverters._

/**
  * Represents a registered connection to a service
  */
sealed trait Registration {
  def connection: Connection
  def port: Int
  def values: Map[String, String]
  def serviceInfo: ServiceInfo = ServiceInfo.create(LocationService.DnsType, connection.toString, port, 0, 0, values.asJava)
}

/**
  * Represents a registered connection to a TCP based service
  */
final case class TcpRegistration(connection: TcpConnection, port: Int) extends Registration {
  override def values: Map[String, String] = Map.empty
}

/**
  * Represents a registered connection to a HTTP based service
  */
final case class HttpRegistration(connection: HttpConnection, port: Int, path: String) extends Registration {
  override def values: Map[String, String] = Map(LocationService.PathKey -> path)
}

/**
  * Represents a registered connection to an Akka service
  */
final case class AkkaRegistration(connection: AkkaConnection, component: ActorRef, prefix: String = "") extends Registration {
  private val uri = new URI(Serialization.serializedActorPath(component))

  override def port: Int = uri.getPort

  override def values: Map[String, String] = {
    Map(
      LocationService.PathKey -> uri.getPath,
      LocationService.SystemKey -> uri.getUserInfo,
      LocationService.PrefixKey -> prefix
    )
  }
}
