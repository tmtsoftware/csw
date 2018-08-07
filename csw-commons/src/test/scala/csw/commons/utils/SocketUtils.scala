package csw.commons.utils
import java.io.IOException
import java.net.ServerSocket

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object SocketUtils {
  @tailrec
  final def getFreePort: Int = {
    Try(new ServerSocket(0)) match {
      case Success(socket) =>
        val port = socket.getLocalPort
        socket.close()
        port
      case Failure(_: IOException) => getFreePort
      case Failure(e)              => throw e
    }
  }
}
