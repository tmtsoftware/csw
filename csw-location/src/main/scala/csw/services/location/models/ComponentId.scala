package csw.services.location.models

/**
  * Used to identify a component
  *
  * @param name          the service name
  * @param componentType HCD, Assembly, Service
  */
case class ComponentId(name: String, componentType: ComponentType)
