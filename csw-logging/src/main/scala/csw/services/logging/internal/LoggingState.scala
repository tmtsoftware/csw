package csw.services.logging.internal

import akka.actor._
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.models.ComponentLoggingState

import scala.collection.mutable
import scala.concurrent.Promise

/**
 * Global state info for logging. Use with care!
 */
private[logging] object LoggingState {

  // This is a default log level applied to all the components, if components does not specify explicitly
  // This value gets overridden by 'logLevel' field from configuration file when logging system is started
  private[logging] var defaultLogLevel = Level("INFO")

  // Queue of messages sent before logger is started
  private[logging] val msgs = new mutable.Queue[LogActorMessages]()

  private[logging] var maybeLogActor: Option[ActorRef] = None
  @volatile private[logging] var loggerStopping        = false

  private[logging] var doTime: Boolean                   = false
  private[logging] var timeActorOption: Option[ActorRef] = None

  // Use to sync akka logging actor shutdown
  private[logging] val akkaStopPromise = Promise[Unit]

  var componentsLoggingState: Map[String, ComponentLoggingState] =
    Map("default" → ComponentLoggingState(defaultLogLevel))
}
