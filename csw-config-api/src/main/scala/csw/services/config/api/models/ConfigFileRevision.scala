package csw.services.config.api.models

import java.time.Instant

/**
 * Holds information about a specific version of a config file
 */
case class ConfigFileRevision private[config] (id: ConfigId, comment: String, time: Instant)
