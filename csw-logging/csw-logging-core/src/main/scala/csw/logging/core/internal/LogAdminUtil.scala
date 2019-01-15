package csw.logging.core.internal

import csw.logging.core.commons.Constants
import csw.logging.core.internal.LoggingLevels.Level
import csw.logging.core.models.LogMetadata

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
