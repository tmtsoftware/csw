package csw.services.location.models

/**
  * Represents a unique component based on `Name` and `ComponentType`.
  *
  * @param name          Name for the `Component`. ''Note :'' Name should not contain
  *            {{{
  *            - leading or trailing spaces
  *            - and hyphen (-)
  *            }}}
  * @param componentType Type for the `Component`
  */
case class ComponentId(name: String, componentType: ComponentType) extends TmtSerializable {

  require(name == name.trim, "component name has leading and trailing whitespaces")

  require(!name.contains("-"), "component name has '-'")
}
