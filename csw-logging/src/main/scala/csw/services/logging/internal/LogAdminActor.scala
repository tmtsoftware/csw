package csw.services.logging.internal

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.services.logging.commons.Constants
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.models.LogMetadata
import csw.services.logging.scaladsl.GenericLogger

object LogAdminActor {
  private[logging] def behavior(): Behavior[LogControlMessages] = Actor.immutable[LogControlMessages] { (ctx, msg) ⇒
    val log = GenericLogger.immutable(ctx)
    log.debug(s"LogAdminActor received message :[$msg]")
    msg match {
      case GetComponentLogMetadata(componentName, replyTo) ⇒ replyTo ! getLogMetadata(componentName)
      case SetComponentLogLevel(componentName, logLevel)   ⇒ setComponentLogLevel(componentName, logLevel)
    }
    Actor.same
  }

  private def getLogMetadata(componentName: String): LogMetadata =
    LogMetadata(
      LoggingState.logLevel,
      LoggingState.akkaLogLevel,
      LoggingState.slf4jLogLevel,
      LoggingState.componentsLoggingState
        .getOrElse(componentName, LoggingState.componentsLoggingState(Constants.DEFAULT_KEY))
        .componentLogLevel
    )

  private def setComponentLogLevel(componentName: String, level: Level): Unit =
    ComponentLoggingStateManager.add(componentName, level)
}
