package csw.location.client.utils

import java.net.Socket

import scala.util.{Failure, Success, Try}

object LocationServerStatus {

  final def up(host: String, port: Int): Boolean = Try(new Socket(host, port)) match {
    case Success(socket) => socket.close(); true
    case Failure(_)      => false
  }

  final def up(locationHost: String): Boolean = up(locationHost, 7654)

  final def requireUp(locationHost: String): Unit =
    require(
      LocationServerStatus.up(locationHost),
      s"Location server is not running at $locationHost:7654. Please check online documentation for location server setup"
    )
}
