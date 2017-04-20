package csw.services.config.models

import java.nio.file.Path

/**
 * Contains information about a config file stored in the config service
 */
case class ConfigFileInfo(path: Path, id: ConfigId, comment: String)
