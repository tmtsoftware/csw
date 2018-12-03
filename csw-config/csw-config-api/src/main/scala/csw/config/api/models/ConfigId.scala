package csw.config.api.models

/**
 * Type of an id returned from ConfigManager create or update methods
 *
 * @param id the string representation of the unique id for the file
 */
case class ConfigId(id: String)

object ConfigId {
  def apply(id: Long): ConfigId = ConfigId(id.toString)
}
