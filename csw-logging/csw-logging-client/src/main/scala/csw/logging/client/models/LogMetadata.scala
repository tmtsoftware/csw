package csw.logging.client.models

import csw.logging.api.models.LoggingLevels.Level
import csw.serializable.LoggingSerializable

/**
 * Holds metadata information about logging configuration
 */
case class LogMetadata(defaultLevel: Level, akkaLevel: Level, slf4jLevel: Level, componentLevel: Level)
    extends LoggingSerializable
