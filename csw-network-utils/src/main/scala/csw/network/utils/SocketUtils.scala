package csw.network.utils

import java.io.IOException
import java.net.{ServerSocket, Socket}

import csw.network.utils.internal.BlockingUtils.poll

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, DurationDouble}
import scala.util.{Failure, Success, Try}

object SocketUtils {

  final def isAddressInUse(host: String = "localhost", port: Int): Boolean = Try(new Socket(host, port)) match {
    case Success(socket) => socket.close(); true
    case Failure(_)      => false
  }

  final def requireServerUp(host: String = "localhost", port: Int, within: Duration = 5.seconds, msg: String): Unit =
    require(poll(SocketUtils.isAddressInUse(host, port)), msg)

  @tailrec
  final def getFreePort: Int = Try(new ServerSocket(0)) match {
    case Success(socket) =>
      val port = socket.getLocalPort
      socket.close()
      port
    case Failure(_: IOException) => getFreePort
    case Failure(e)              => throw e
  }
}
