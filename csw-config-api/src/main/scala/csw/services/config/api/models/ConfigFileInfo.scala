package csw.services.config.api.models

import java.nio.file.Path

/**
 * Contains information about a config file stored in the config service
 */
case class ConfigFileInfo private[config] (path: Path, id: ConfigId, comment: String)
