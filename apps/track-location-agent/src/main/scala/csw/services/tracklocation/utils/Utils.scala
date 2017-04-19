package csw.services.tracklocation.utils

import java.io.File
import java.net.ServerSocket

import com.typesafe.config.{Config, ConfigFactory, ConfigResolveOptions}

object Utils {

  /**
   * Finds a random, free port to use
   */
  def getFreePort: Int = {
    val sock = new ServerSocket(0)
    val port = sock.getLocalPort
    sock.close()
    port
  }

  /**
   * Returns an `Option` of `Config` pointed by the file parameter, else None.
   */
  def getAppConfig(file: File): Option[Config] =
    if (file.exists()) Some(ConfigFactory.parseFileAnySyntax(file).resolve(ConfigResolveOptions.noSystem())) else None
}
