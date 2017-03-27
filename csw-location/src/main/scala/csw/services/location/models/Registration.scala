package csw.services.location.models

import java.net.URI

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

/**
  * Represents a registered connection to a service
  */
sealed abstract class Registration {
  def connection: Connection
  def location(hostname: String): Location
}

/**
  * Represents a registered connection to an Akka service
  */
final case class AkkaRegistration(connection: AkkaConnection, actorRef: ActorRef) extends Registration {
  private val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))

  override def location(hostname: String): Location = AkkaLocation(connection, new URI(actorPath.toString), actorRef)
}

/**
  * Represents a registered connection to a TCP based service
  */
final case class TcpRegistration(connection: TcpConnection, port: Int) extends Registration {
  override def location(hostname: String): Location = TcpLocation(connection, new URI(s"tcp://$hostname:$port"))
}

/**
  * Represents a registered connection to a HTTP based service
  */
final case class HttpRegistration(connection: HttpConnection, port: Int, path: String) extends Registration {
  override def location(hostname: String): Location = HttpLocation(connection, new URI(s"http://$hostname:$port/$path"))
}
