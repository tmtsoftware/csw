package csw.services.logging.internal

import com.typesafe.config.Config
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.models.ComponentLoggingState

import scala.collection.JavaConverters._
import scala.util.Try

private[logging] object ComponentLoggingStateManager {

  /**
   * Extracts the component-log-levels from logging configuration. It takes the config properties and stores in the map
   * componentName ->
   *
   * @param loggingConfig the logging configuration object
   * @return Set of Filters
   */
  def from(loggingConfig: Config): Map[String, ComponentLoggingState] =
    Try {
      loggingConfig
        .getObject("component-log-levels")
        .unwrapped()
        .asScala
        .map {
          case (name, componentLogLevel) ⇒ (name, ComponentLoggingState(Level(componentLogLevel.toString)))
        }
        .toMap
    }.getOrElse(Map.empty)

  /**
   * Add the component logging state for a component in map componentName -> ComponentLoggingState
   *
   * @param componentName The name of the component
   * @param level The log level for the component
   */
  def add(componentName: String, level: Level): Unit = {
    import csw.services.logging.internal.LoggingState._
    componentsLoggingState = componentsLoggingState ++ Map(componentName → ComponentLoggingState(level))
  }
}
