package csw.services.logging.models

import csw.services.logging.internal.LoggingLevels.Level

/**
 * Holds metadata information about logging configuration
 */
case class LogMetadata(level: Level, akkaLevel: Level, slf4jLevel: Level, filters: FilterSet)
