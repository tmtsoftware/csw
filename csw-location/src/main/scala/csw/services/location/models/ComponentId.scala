package csw.services.location.models

import scala.util.Try

/**
 * Used to identify a component
 *
 * @param name the service name
 * @param componentType HCD, Assembly, Service
 */
case class ComponentId(name: String, componentType: ComponentType) {
  override def toString = s"$name-$componentType"
}

object ComponentId {
  /**
   * Gets a ComponentId from a string, as output by ComponentId.toString
   */
  def parse(s: String): Try[ComponentId] = {
    val (name, typ) = s.splitAt(s.lastIndexOf('-'))
    ComponentType.parse(typ.drop(1)).map(ComponentId(name, _))
  }
}

