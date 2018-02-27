package csw.services.logging.javadsl

import java.net.InetAddress

import akka.actor.ActorSystem
import csw.services.logging.appenders.LogAppenderBuilder
import csw.services.logging.internal.LoggingSystem

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object JLoggingSystemFactory {
  //To be used only for testing only
  def start(): LoggingSystem = {
    new LoggingSystem("foo-name", "foo-version", InetAddress.getLocalHost.getHostName, ActorSystem("logging"))
  }

  def start(name: String, version: String, hostName: String, actorSystem: ActorSystem): LoggingSystem =
    new LoggingSystem(name, version, hostName, actorSystem)

  def start(
      name: String,
      version: String,
      hostName: String,
      actorSystem: ActorSystem,
      appenders: java.util.List[LogAppenderBuilder]
  ): LoggingSystem = {
    val loggingSystem = new LoggingSystem(name, version, hostName, actorSystem)
    loggingSystem.setAppenders(appenders.asScala.toList)
    loggingSystem
  }
}
