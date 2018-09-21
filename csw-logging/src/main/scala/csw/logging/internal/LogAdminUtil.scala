package csw.logging.internal

import csw.logging.commons.Constants
import csw.logging.internal.LoggingLevels.Level
import csw.logging.models.LogMetadata

// Helper methods to get/set the log level of components
private[csw] object LogAdminUtil {

  private[csw] def getLogMetadata(componentName: String): LogMetadata =
    LogMetadata(
      LoggingState.logLevel,
      LoggingState.akkaLogLevel,
      LoggingState.slf4jLogLevel,
      LoggingState.componentsLoggingState
        .getOrElse(componentName, LoggingState.componentsLoggingState(Constants.DEFAULT_KEY))
        .componentLogLevel
    )

  private[csw] def setComponentLogLevel(componentName: String, level: Level): Unit =
    ComponentLoggingStateManager.add(componentName, level)
}
