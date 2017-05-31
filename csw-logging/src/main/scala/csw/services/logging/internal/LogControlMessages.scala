package csw.services.logging.internal

import csw.services.logging.internal.LoggingLevels.Level

sealed trait LogControlMessages

case object GetComponentLogMetadata extends LogControlMessages

case class SetComponentLogLevel(logLevel: Level) extends LogControlMessages
