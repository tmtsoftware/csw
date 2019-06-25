package csw.logging.client.internal

import csw.logging.api.models.Level
import csw.logging.client.commons.Constants
import csw.logging.client.models.LogMetadata

// Helper methods to get/set the log level of components
private[csw] object LogAdminUtil {

  private[csw] def getLogMetadata(componentName: String): LogMetadata =
    LogMetadata(
      LoggingState.logLevel,
      LoggingState.akkaLogLevel,
      LoggingState.slf4jLogLevel,
      LoggingState.componentsLoggingState
        .getOrDefault(componentName, LoggingState.componentsLoggingState.get(Constants.DEFAULT_KEY))
        .componentLogLevel
    )

  private[csw] def setComponentLogLevel(componentName: String, level: Level): Unit =
    ComponentLoggingStateManager.add(componentName, level)
}
