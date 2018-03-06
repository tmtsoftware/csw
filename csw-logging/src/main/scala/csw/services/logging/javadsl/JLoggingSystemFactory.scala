package csw.services.logging.javadsl

import java.net.InetAddress

import akka.actor.ActorSystem
import csw.services.logging.appenders.LogAppenderBuilder
import csw.services.logging.internal.LoggingSystem

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object JLoggingSystemFactory {

  /**
   * The factory is used to create LoggingSystem instance. This method will be used by `csw-framework` to create and
   * start the LoggingSystem.
   *
   * @param name
   * @param version
   * @param hostName
   * @param actorSystem
   * @return
   */
  def start(name: String, version: String, hostName: String, actorSystem: ActorSystem): LoggingSystem =
    new LoggingSystem(name, version, hostName, actorSystem)

  /**
   *
   * @return
   */
  private[csw] def start(): LoggingSystem = {
    new LoggingSystem("foo-name", "foo-version", InetAddress.getLocalHost.getHostName, ActorSystem("logging"))
  }

  /**
   *
   * @param name
   * @param version
   * @param hostName
   * @param actorSystem
   * @param appenders
   * @return
   */
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
