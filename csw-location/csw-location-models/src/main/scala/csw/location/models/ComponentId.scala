package csw.location.models

import csw.prefix.models.Prefix

/**
 * Represents a component based on its prefix and type.
 *
 * @note Prefix should not contain
 *  - leading or trailing spaces
 *  - and hyphen (-)
 *  @param prefix represents the prefix (subsystem and name) of the component e.g. tcs.filter.wheel
 *  @param componentType represents a type of the Component e.g. Assembly, HCD, Sequencer etc
 */
case class ComponentId private[location] (prefix: Prefix, componentType: ComponentType) {

  /**
   * Represents the name and componentType
   */
  def fullName: String = s"$prefix-${componentType.name}"
}
