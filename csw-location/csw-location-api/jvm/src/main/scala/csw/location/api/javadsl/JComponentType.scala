package csw.location.api.javadsl

import csw.location.models.ComponentType

/**
 * Helper class for Java to get the handle of `ComponentType` which is fundamental to LocationService library
 */
object JComponentType {

  /**
   * A container for components (assemblies and HCDs)
   */
  val Container: ComponentType = ComponentType.Container

  /**
   * A component that controls a hardware device
   */
  val HCD: ComponentType = ComponentType.HCD

  /**
   * A component that controls one or more HCDs or assemblies
   */
  val Assembly: ComponentType = ComponentType.Assembly

  /**
   * A component that controls one or more sequencers or assemblies
   */
  val Sequencer: ComponentType = ComponentType.Sequencer

  /**
   * A sequence component e.g ocs_1, iris_1
   */
  val SequenceComponent: ComponentType = ComponentType.SequenceComponent

  /**
   * A general purpose service component (actor and/or web service application)
   */
  val Service: ComponentType = ComponentType.Service
}
