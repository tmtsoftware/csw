package csw.services.logging.scaladsl

import java.net.InetAddress

import csw.services.logging.appenders.StdOutAppender

object LoggingSystemFactory extends GenericLogger.Simple {
  def start(): LoggingSystem =
    new LoggingSystem("serviceName1", "serviceVersion1", InetAddress.getLocalHost.getHostName,
      appenderBuilders = Seq(StdOutAppender))
}
