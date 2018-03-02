package csw.services.config.api.models

import java.nio.file.Path

/**
 * Contains information about a config file stored in the config service
 *
 * @param path The path of file sitting in config service
 * @param id The ConfigId representing unique id of the file
 * @param comment The comment end user wants to provide while committing the file in config service
 */
case class ConfigFileInfo private[config] (path: Path, id: ConfigId, comment: String)
