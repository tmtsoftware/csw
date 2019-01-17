package csw.logging.client.javadsl

import java.net.InetAddress

import akka.actor.ActorSystem
import csw.logging.client.appenders.LogAppenderBuilder
import csw.logging.client.internal.LoggingSystem

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object JLoggingSystemFactory {

  /**
   * The factory used to create the LoggingSystem. `LoggingSystem` should be started once in an app.
   *
   * @param name The name of the logging system. If there is a file appender configured, then a file with this name is
   *             created on local machine.
   * @param version the version of the csw which will be a part of log statements
   * @param hostName the host address which will be a part of log statements
   * @param actorSystem the ActorSystem used to create LogActor from LoggingSystem
   * @return the instance of LoggingSystem
   */
  def start(name: String, version: String, hostName: String, actorSystem: ActorSystem): LoggingSystem =
    new LoggingSystem(name, version, hostName, actorSystem)

  /**
   * The factory used to create the LoggingSystem. `LoggingSystem` should be started once in an app.
   *
   * @note it is recommended to use this method for only testing
   * @return the instance of LoggingSystem
   */
  private[csw] def start(): LoggingSystem =
    new LoggingSystem("foo-name", "foo-version", InetAddress.getLocalHost.getHostName, ActorSystem("logging"))

  /**
   * The factory used to create the LoggingSystem. `LoggingSystem` should be started once in an app.
   *
   * @param name The name of the logging system. If there is a file appender configured, then a file with this name is
   *             created on local machine.
   * @param version the version of the csw which will be a part of log statements
   * @param hostName the host address which will be a part of log statements
   * @param actorSystem the ActorSystem used to create LogActor from LoggingSystem
   * @param appenders the list of appenders given programmatically
   * @note it is recommended to use this method for only testing
   * @return the instance of LoggingSystem
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
