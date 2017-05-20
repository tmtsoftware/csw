package csw.services.logging

import java.net.InetAddress

object LoggingSystemFactory {
  def make(): LoggingSystem =
    LoggingSystem("serviceName1", "serviceVersion1", InetAddress.getLocalHost.getHostName, Seq(StdOutAppender))
}
