package csw.logging.models
import csw.logging.internal.LoggingLevels.Level
import csw.serializable.LoggingSerializable

/**
 * Holds metadata information about logging configuration
 */
case class LogMetadata(defaultLevel: Level, akkaLevel: Level, slf4jLevel: Level, componentLevel: Level)
    extends LoggingSerializable
