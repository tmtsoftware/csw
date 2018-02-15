package csw.services.event.helpers

import java.io.IOException
import java.net.ServerSocket

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object PortHelper {
  @tailrec
  def freePort: Int = {
    Try(new ServerSocket(0)) match {
      case Success(socket) =>
        val port = socket.getLocalPort
        socket.close()
        port
      case Failure(e: IOException) ⇒ freePort
      case Failure(e)              ⇒ throw e
    }
  }
}
