package csw.logging.client.internal

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.Config
import csw.logging.client.models.ComponentLoggingState
import csw.logging.models.Level
import csw.prefix.models.Prefix

import scala.jdk.CollectionConverters._
import scala.util.Try

private[logging] object ComponentLoggingStateManager {

  /**
   * Extracts the component-log-levels from logging configuration. It takes the config properties and stores in the map
   * componentName ->
   *
   * @param loggingConfig the logging configuration object
   * @return set of Filters
   */
  def from(loggingConfig: Config): ConcurrentHashMap[Prefix, ComponentLoggingState] = {
    val stringToState = Try {
      loggingConfig
        .getConfig("component-log-levels")
        .entrySet()
        .asScala
        .map { entry =>
          (entry.getKey, ComponentLoggingState(Level(entry.getValue.unwrapped().toString)))
        }
        .toMap
        .map[Prefix, ComponentLoggingState] {
          case (k, v) => (Prefix(k), v)
        }
    }.getOrElse(Map.empty)
    new ConcurrentHashMap(stringToState.asJava)
  }

  /**
   * Add the component logging state for a component in map componentName -> ComponentLoggingState
   *
   * @param prefix the subsystem and componentName of the component e.g. tcs.filter.wheel
   * @param level the log level for the component
   */
  def add(prefix: Prefix, level: Level): Unit = {
    import csw.logging.client.internal.LoggingState._
    componentsLoggingState.put(prefix, ComponentLoggingState(level))
  }
}
