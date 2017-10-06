package csw.services.logging.scaladsl

import akka.typed.Behavior
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.services.logging.commons.Constants
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.internal._
import csw.services.logging.models.LogMetadata

class LogAdminActor(ctx: ActorContext[LogControlMessages]) extends GenericLogger.TypedActor(ctx) {
  override def onMessage(msg: LogControlMessages): Behavior[LogControlMessages] = {
    msg match {
      case GetComponentLogMetadata(componentName, replyTo) ⇒ replyTo ! getLogMetadata(componentName)
      case SetComponentLogLevel(componentName, logLevel)   ⇒ setComponentLogLevel(componentName, logLevel)
    }
    this
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

object LogAdminActor {
  def behavior(): Behavior[LogControlMessages] = Actor.mutable[LogControlMessages](ctx ⇒ new LogAdminActor(ctx))
}
