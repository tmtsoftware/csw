package csw.services.logging.internal

import com.typesafe.config.Config
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.models.ComponentLoggingState

import scala.collection.JavaConverters._
import scala.util.Try

object ComponentLoggingStateManager {

  /**
   * Extracts the set of filters configured in logging configuration
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
          case (name, filterLevel) ⇒ {
            (name, ComponentLoggingState(Level(filterLevel.toString)))
          }
        }
        .toMap
    }.getOrElse(Map.empty)

  def add(componentName: String, level: Level): Unit = {
    import csw.services.logging.internal.LoggingState._
    componentsLoggingState = componentsLoggingState ++ Map(componentName → ComponentLoggingState(level))
  }
}
