package csw.services.location.models

import java.net.URI

import akka.actor.{ActorPath, ActorRef, Address}
import akka.serialization.Serialization
import csw.services.location.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

/**
  * Registration holds information about a connection and its live location. This model is used to register a connection with LocationService.
  */
sealed abstract class Registration {
  def connection: Connection
  def location(hostname: String): Location
}

/**
  * AkkaRegistration holds the information needed to register an akka location
  *
  * @param actorRef Provide a remote actor that is offering a connection. Local actors cannot be registered since they can't be
  *                 communicated from components across the network
  */
final case class AkkaRegistration(connection: AkkaConnection, actorRef: ActorRef) extends Registration {

  // ActorPath represents the akka path of an Actor
  private val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))

  // Prepare the URI from the ActorPath. Allow only the remote actor to be registered with LocationService
  private val uri = {
    actorPath.address match {
      case Address(_, _, None, None) => throw LocalAkkaActorRegistrationNotAllowed(actorRef)
      case _ => new URI(actorPath.toString)
    }
  }

  /**
    * Create a AkkaLocation that represents the live connection offered by the actor
    */
  override def location(hostname: String): Location = AkkaLocation(connection, uri, actorRef)
}

/**
  * TcpRegistration holds information needed to register a Tcp service
  *
  * @param port Provide the port where Tcp service is available
  */
final case class TcpRegistration(connection: TcpConnection, port: Int) extends Registration {

  /**
    * Create a TcpLocation that represents the live Tcp service
    *
    * @param hostname Provide the hostname where Tcp service is available
    */
  override def location(hostname: String): Location = TcpLocation(connection, new URI(s"tcp://$hostname:$port"))
}

/**
  * HttpRegistration holds information needed to register a Http service
  *
  * @param port Provide the port where Http service is available
  * @param path Provide the path to reach the available http service
  */
final case class HttpRegistration(connection: HttpConnection, port: Int, path: String) extends Registration {

  /**
    * Create a HttpLocation that represents the live Http service
    *
    * @param hostname  Provide the hostname where Http service is available
    */
  override def location(hostname: String): Location = HttpLocation(connection, new URI(s"http://$hostname:$port/$path"))
}
