package csw.services.logging.internal

import akka.typed.ActorRef
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.models.LogMetadata

// Parent trait for Messages which will be send to components for interacting with its logging system
sealed trait LogControlMessages

// Message to get Logging configuration metadata of the receiver
case class GetComponentLogMetadata(componentName: String, replyTo: ActorRef[LogMetadata]) extends LogControlMessages

// Message to change the log level of any component
case class SetComponentLogLevel(componentName: String, logLevel: Level) extends LogControlMessages
