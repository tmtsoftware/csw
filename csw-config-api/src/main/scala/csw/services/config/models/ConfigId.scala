package csw.services.config.models

/**
 * Type of an id returned from ConfigManager create or update methods
 */
case class ConfigId(id: String)

object ConfigId {
  def apply(id: Long): ConfigId = ConfigId(id.toString)
}
