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
   * @param prefixHandle represents full name (from prefix) of component whose LogMetadata needs to be fetched
   * @return LogMetaData of the given component
   */
  def getLogMetadata(prefixHandle: String): LogMetadata =
    LogMetadata(
      LoggingState.logLevel,
      LoggingState.akkaLogLevel,
      LoggingState.slf4jLogLevel,
      LoggingState.componentsLoggingState
        .getOrDefault(prefixHandle, LoggingState.componentsLoggingState.get(Constants.DEFAULT_KEY))
        .componentLogLevel
    )

  /**
   * Updates the log level of component
   *
   * @param prefixHandle represents full name (from prefix) of component whose log level to be changed
   * @param level represents log level to set
   */
  def setComponentLogLevel(prefixHandle: String, level: Level): Unit =
    ComponentLoggingStateManager.add(prefixHandle, level)
}
