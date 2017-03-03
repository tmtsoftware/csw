package csw.services.tracklocation.utils

import java.net.ServerSocket

import com.typesafe.config.Config
import csw.services.tracklocation.models.Options

object PortOptHandler {

  // Find a random, free port to use
  def getFreePort: Int = {
    val sock = new ServerSocket(0)
    val port = sock.getLocalPort
    sock.close()
    port
  }

  // Use the value of the --port option, or use a random, free port
  def apply(portKey:String, portValue: Option[Int], options: Options, appConfig: Option[Config]): Int ={
    IntOptHandler(portKey, portValue, options, appConfig).getOrElse(getFreePort)
  }
}