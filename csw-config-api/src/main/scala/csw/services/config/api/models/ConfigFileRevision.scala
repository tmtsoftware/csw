package csw.services.config.api.models

import java.time.Instant

/**
 * Holds information about a specific version of a config file
 */
case class ConfigFileRevision(id: ConfigId, comment: String, time: Instant)
