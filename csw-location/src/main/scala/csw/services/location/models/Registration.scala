package csw.services.location.models

import java.net.URI

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

/**
  * Represents a [[csw.services.location.models.Connection]] and it's associated [[csw.services.location.models.Location]]
  * to register with [[csw.services.location.scaladsl.LocationService]]
  */
sealed abstract class Registration {

  def connection: Connection

  /**
    * A `Location` associated with the `Connection`
    *
    * @param hostname A `Location` is picked based on provided `hostname`
    */
  def location(hostname: String): Location
}

/**
  * Represents an `AkkaConnection` and `ActorRef` with it's associated `Location`
  */
final case class AkkaRegistration(connection: AkkaConnection, actorRef: ActorRef) extends Registration {

  /**
    * INTERNAL API : `ActorPath` derived from serializing the given `ActorRef`
    */
  private val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))

  /**
    * A [[csw.services.location.models.AkkaLocation]] is formed with the given `Connection` and  `URI`.
    * The `URI` is derived from `ActorPath` of the given `ActorRef`.
    */
  override def location(hostname: String): Location = AkkaLocation(connection, new URI(actorPath.toString), actorRef)
}

/**
  * Represents a `TcpConnection` and `port` with it's associated `Location` for a Tcp based service
  */
final case class TcpRegistration(connection: TcpConnection, port: Int) extends Registration {

  /**
    * A [[csw.services.location.models.TcpLocation]] is formed with the `Connection` and `URI`.
    * The `URI` is derived from `hostname` and `port`.
    */
  override def location(hostname: String): Location = TcpLocation(connection, new URI(s"tcp://$hostname:$port"))
}

/**
  * Represents a `HttpConnection`, `port` and `path` with it's associated `Location` for a Http based service
  */
final case class HttpRegistration(connection: HttpConnection, port: Int, path: String) extends Registration {

  /**
    * A [[csw.services.location.models.HttpLocation]] is formed with the `Connection` and `URI`.
    * The `URI` is derived from `hostname`, `port` and `path`.
    */
  override def location(hostname: String): Location = HttpLocation(connection, new URI(s"http://$hostname:$port/$path"))
}
