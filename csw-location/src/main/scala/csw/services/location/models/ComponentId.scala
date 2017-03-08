package csw.services.location.models

/**
  * Used to identify a component
  *
  * @param name          the service name
  * @param componentType HCD, Assembly, Service
  */
case class ComponentId(name: String, componentType: ComponentType) {
  //jmDNS auto-trims names which leads to surprising effects during unregistration
  require(name == name.trim, "component name does not have leading and trailing whitespaces")
  require(!name.contains("-"), "component name does not have '-'")
}
