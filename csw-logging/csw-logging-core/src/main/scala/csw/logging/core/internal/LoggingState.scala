package csw.logging.core.internal

import akka.actor._
import csw.logging.core.commons.Constants
import csw.logging.core.internal.LoggingLevels.Level
import csw.logging.core.models.ComponentLoggingState

import scala.collection.mutable
import scala.concurrent.Promise

/**
 * Global state info for logging. Use with care!
 */
private[logging] object LoggingState {

  // This is a default log level applied to all the components, if components does not specify explicitly
  // This value gets overridden by 'logLevel' field from configuration file when logging system is started
  private[logging] var defaultLogLevel = Level("INFO")

  private[logging] var logLevel: Level      = _
  private[logging] var akkaLogLevel: Level  = _
  private[logging] var slf4jLogLevel: Level = _
  // queue of messages sent before logger is started
  private[logging] val msgs = new mutable.Queue[LogActorMessages]()

  // LogActor that gets instantiated when LoggingSystem starts
  private[logging] var maybeLogActor: Option[ActorRef] = None
  @volatile private[logging] var loggerStopping        = false

  private[logging] var doTime: Boolean                   = false
  private[logging] var timeActorOption: Option[ActorRef] = None

  // use to sync akka logging actor shutdown
  private[logging] val akkaStopPromise = Promise[Unit]

  // a map of componentName -> LoggingState
  var componentsLoggingState: Map[String, ComponentLoggingState] =
    Map(Constants.DEFAULT_KEY → ComponentLoggingState(defaultLogLevel))
}
