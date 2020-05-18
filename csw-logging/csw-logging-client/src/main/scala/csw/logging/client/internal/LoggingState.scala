package csw.logging.client.internal

import java.util.concurrent.ConcurrentHashMap

import akka.actor.typed.ActorRef
import csw.logging.client.internal.TimeActorMessages.TimeActorMessage
import csw.logging.client.models.ComponentLoggingState
import csw.logging.models.Level
import csw.prefix.models.Prefix

import scala.collection.mutable
import scala.concurrent.Promise

/**
 * Global state info for logging. Use with care!
 */
private[logging] object LoggingState {

  // This is a default log level applied to all the components, if components does not specify explicitly
  // This value gets overridden by 'logLevel' field from configuration file when logging system is started
  var defaultLogLevel = Level("INFO")

  var logLevel: Level      = _
  var akkaLogLevel: Level  = _
  var slf4jLogLevel: Level = _
  // queue of messages sent before logger is started
  val msgs = new mutable.Queue[LogActorMessages]()

  // LogActor that gets instantiated when LoggingSystem starts
  var maybeLogActor: Option[ActorRef[LogActorMessages]] = None
  @volatile var loggerStopping                          = false

  var doTime: Boolean                                     = false
  var timeActorOption: Option[ActorRef[TimeActorMessage]] = None

  // use to sync akka logging actor shutdown
  val akkaStopPromise: Promise[Unit] = Promise[Unit]()

  // a concurrent map of prefix -> LoggingState
  val componentsLoggingState: ConcurrentHashMap[Prefix, ComponentLoggingState] = new ConcurrentHashMap()
}
