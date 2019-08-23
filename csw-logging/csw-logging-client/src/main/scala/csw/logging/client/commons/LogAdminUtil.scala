package csw.logging.client.commons

import csw.logging.client.internal.{ComponentLoggingStateManager, LoggingState}
import csw.logging.models.{Level, LogMetadata}

/**
 * Helper to get/set the log level of components
 */
object LogAdminUtil {

  /**
   * Fetches the LogMetadata for given component
   *
   * @param componentName represents name of component whose LogMetadata needs to be fetched
   * @return LogMetaData of the given component
   */
  def getLogMetadata(componentName: String): LogMetadata =
    LogMetadata(
      LoggingState.logLevel,
      LoggingState.akkaLogLevel,
      LoggingState.slf4jLogLevel,
      LoggingState.componentsLoggingState
        .getOrDefault(componentName, LoggingState.componentsLoggingState.get(Constants.DEFAULT_KEY))
        .componentLogLevel
    )

  /**
   * Updates the log level of component
   *
   * @param componentName represents name of component whose log level to be changed
   * @param level represents log level to set
   */
  def setComponentLogLevel(componentName: String, level: Level): Unit =
    ComponentLoggingStateManager.add(componentName, level)
}
