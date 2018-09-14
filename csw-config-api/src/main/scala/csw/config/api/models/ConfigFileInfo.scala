package csw.config.api.models

import java.nio.file.Path

/**
 * Contains information about a config file stored in the config service
 *
 * @param path the path of file sitting in config service
 * @param id the ConfigId representing unique id of the file
 * @param comment the comment end user wants to provide while committing the file in config service
 */
case class ConfigFileInfo private[config] (path: Path, id: ConfigId, comment: String)
