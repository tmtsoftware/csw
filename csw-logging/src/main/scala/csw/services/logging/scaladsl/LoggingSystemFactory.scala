package csw.services.logging.scaladsl

import java.net.InetAddress

import akka.actor.ActorSystem
import csw.services.logging.internal.LoggingSystem

object LoggingSystemFactory {
  //To be used for testing purpose only
  private[logging] def start(): LoggingSystem =
    new LoggingSystem("foo-name", "foo-version", InetAddress.getLocalHost.getHostName, ActorSystem("logging"))

  def start(name: String, version: String, hostName: String, actorSystem: ActorSystem): LoggingSystem =
    new LoggingSystem(name, version, hostName, actorSystem)
}
