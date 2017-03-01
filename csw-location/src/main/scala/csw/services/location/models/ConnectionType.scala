package csw.services.location.models

import scala.util.{Failure, Success, Try}

/**
 * Connection type: Indicate if it is an http server or an akka actor.
 */
sealed trait ConnectionType {
  def name: String

  override def toString = name
}

object ConnectionType {

  /**
   * Type of a REST/HTTP based service
   */
  case object HttpType extends ConnectionType {
    val name = "http"
  }

  /**
   * Type of a TCP based service
   */
  case object TcpType extends ConnectionType {
    val name = "tcp"
  }

  /**
   * Type of an Akka actor based service
   */
  case object AkkaType extends ConnectionType {
    val name = "akka"
  }

  /**
   * Exception throws for an unknown connection type
   */
  case class UnknownConnectionTypeException(message: String) extends Exception(message)

  /**
   * Gets a ConnectionType from the string value ("akka" or "http") or an UnknownConnectionTypeException
   */
  def parse(name: String): Try[ConnectionType] = name match {
    case "http" => Success(HttpType)
    case "akka" => Success(AkkaType)
    case "tcp"  => Success(TcpType)
    case x      => Failure(UnknownConnectionTypeException(x))
  }

}

