package csw.messages.location

import csw.messages.TMTSerializable
import play.api.libs.json.{Format, Json}

/**
 * Represents a component based on its name and type.
 *
 * @note Name should not contain
 *  - leading or trailing spaces
 *  - and hyphen (-)
 *  @param name represents the unique name of the component
 *  @param componentType represents a type of the Component e.g. Assembly, HCD, Sequencer etc
 */
case class ComponentId private[messages] (name: String, componentType: ComponentType) extends TMTSerializable {

  /**
   * Represents the name and componentType
   */
  def fullName: String = s"$name-${componentType.name}"

  require(name == name.trim, "component name has leading and trailing whitespaces")

  require(!name.contains("-"), "component name has '-'")
}

object ComponentId {
  import upickle.default.{macroRW, ReadWriter ⇒ RW}
  private[messages] implicit val format: Format[ComponentId] = Json.format
  implicit def componentIdRw: RW[ComponentId]                = macroRW
}
