package csw.services.config.models

/**
 * Type of an id returned from ConfigManager create or update methods
 */
trait ConfigId {
  val id: String
}

object ConfigId {
  def apply(id: String): ConfigId = ConfigIdImpl(id)

  def apply(id: Long): ConfigId = ConfigIdImpl(id.toString)
}

/**
  * Type of an id returned from ConfigManager create or update methods.
  */
case class ConfigIdImpl(id: String) extends ConfigId
