package csw.services.logging.scaladsl

import akka.typed.Behavior
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.services.logging.internal.{GetComponentLogMetadata, LogControlMessages, LoggingSystem, SetComponentLogLevel}

class LogAdminActor(ctx: ActorContext[LogControlMessages], loggingSystem: LoggingSystem)
    extends GenericLogger.TypedActor(ctx) {
  override def onMessage(msg: LogControlMessages): Behavior[LogControlMessages] = {
    msg match {
      case GetComponentLogMetadata(componentName, replyTo) ⇒ replyTo ! loggingSystem.getLogMetadata(componentName)
      case SetComponentLogLevel(componentName, logLevel)   ⇒ loggingSystem.setComponentLogLevel(componentName, logLevel)
    }
    this
  }
}

object LogAdminActor {
  def behavior(loggingSystem: LoggingSystem): Behavior[LogControlMessages] =
    Actor.mutable[LogControlMessages](ctx ⇒ new LogAdminActor(ctx, loggingSystem))
}
