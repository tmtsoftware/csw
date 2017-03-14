package csw.services.location.scaladsl.models

/**
  * Used to identify a component
  *
  * @param name          the service name
  * @param componentType HCD, Assembly, Service
  */
case class ComponentId(name: String, componentType: ComponentType) {
  //jmDNS auto-trims names which leads to surprising effects during unregistration
  require(name == name.trim, "component name has leading and trailing whitespaces")

  //'-' in the name leads to confusing connection strings in the UI listing services
  require(!name.contains("-"), "component name has '-'")
}
