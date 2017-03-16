package csw.services.location.models

import javax.jmdns.ServiceInfo

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.common.Constants

import scala.collection.JavaConverters._

/**
  * Represents a registered connection to a service
  */
sealed abstract class Registration {
  def connection: Connection
  def port: Int
  def values: Map[String, String]

  def serviceInfo: ServiceInfo = ServiceInfo.create(Constants.DnsType, connection.name, port, 0, 0, values.asJava)
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
  override def values: Map[String, String] = Map(
    Constants.PathKey -> path
  )
}

/**
  * Represents a registered connection to an Akka service
  */
final case class AkkaRegistration(connection: AkkaConnection, component: ActorRef, prefix: String = "") extends Registration {
  private val actorPath = ActorPath.fromString(Serialization.serializedActorPath(component))

  override def port: Int = {
    actorPath.address.port.getOrElse(throw new RuntimeException(s"missing port for actorRef=$component"))
  }

  override def values: Map[String, String] = {
    Map(
      Constants.ActorPathKey -> actorPath.toSerializationFormat,
      Constants.PrefixKey -> prefix
    )
  }
}
