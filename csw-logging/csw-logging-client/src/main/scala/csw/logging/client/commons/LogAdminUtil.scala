package csw.logging.client.commons

import csw.logging.client.internal.{ComponentLoggingStateManager, LoggingState}
import csw.logging.client.models.ComponentLoggingState
import csw.logging.models.{Level, LogMetadata}
import csw.prefix.models.Prefix

/**
 * Helper to get/set the log level of components
 */
object LogAdminUtil {

  /**
   * Fetches the LogMetadata for given component
   *
   * @param prefix represents full name (from prefix) of component whose LogMetadata needs to be fetched
   * @return LogMetaData of the given component
   */
  def getLogMetadata(prefix: Prefix): LogMetadata = LogMetadata(
    LoggingState.logLevel,
    LoggingState.akkaLogLevel,
    LoggingState.slf4jLogLevel,
    LoggingState.componentsLoggingState
      .getOrDefault(prefix, ComponentLoggingState(LoggingState.defaultLogLevel))
      .componentLogLevel
  )

  /**
   * Updates the log level of component
   *
   * @param prefix represents full name (from prefix) of component whose log level to be changed
   * @param level represents log level to set
   */
  def setComponentLogLevel(prefix: Prefix, level: Level): Unit =
    ComponentLoggingStateManager.add(prefix, level)
}
