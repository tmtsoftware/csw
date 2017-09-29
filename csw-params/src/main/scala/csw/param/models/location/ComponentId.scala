package csw.param.models.location

import csw.param.TMTSerializable
import play.api.libs.json.{Format, Json}

/**
 * Represents a component based on its name and type.
 *
 * ''Note : '' Name should not contain
 *  - leading or trailing spaces
 *  - and hyphen (-)
 */
case class ComponentId(name: String, componentType: ComponentType) extends TMTSerializable {

  def fullName: String = s"$name-${componentType.name}"

  require(name == name.trim, "component name has leading and trailing whitespaces")

  require(!name.contains("-"), "component name has '-'")
}

object ComponentId {
  implicit val format: Format[ComponentId] = Json.format
}
