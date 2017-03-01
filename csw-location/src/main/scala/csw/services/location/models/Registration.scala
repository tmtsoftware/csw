package csw.services.location.models

import akka.actor.ActorRef
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

/**
  * Represents a registered connection to a service
  */
sealed trait Registration {
  def connection: Connection
}

/**
  * Represents a registered connection to an Akka service
  */
final case class AkkaRegistration(connection: AkkaConnection, component: ActorRef, prefix: String = "") extends Registration

/**
  * Represents a registered connection to a HTTP based service
  */
final case class HttpRegistration(connection: HttpConnection, port: Int, path: String) extends Registration

/**
  * Represents a registered connection to a TCP based service
  */
final case class TcpRegistration(connection: TcpConnection, port: Int) extends Registration
