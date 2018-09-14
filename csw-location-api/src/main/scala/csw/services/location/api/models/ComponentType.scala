package csw.services.location.api.models

import csw.params.TMTSerializable
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable.IndexedSeq

/**
 * Represents a type of the Component. It should be serializable since it has to be transmittable over the network.
 * The type will always be represented in lower case.
 *
 * @param messageManifest represents the class name of message that a component will understand
 */
sealed abstract class ComponentType(val messageManifest: String) extends EnumEntry with Lowercase with TMTSerializable {

  /**
   * The name of ComponentType e.g. for HCD components, the name will be represented as `hcd`.
   */
  def name: String = entryName
}

object ComponentType extends Enum[ComponentType] with PlayJsonEnum[ComponentType] {

  /**
   * Returns a sequence of all component types
   */
  def values: IndexedSeq[ComponentType] = findValues

  /**
   * Represents a container for components e.g assemblies and HCDs
   */
  case object Container extends ComponentType("ContainerMessage")

  /**
   * Represents a component that controls a hardware device
   */
  case object HCD extends ComponentType("ComponentMessage")

  /**
   * Represents a component that controls one or more HCDs or assemblies
   */
  case object Assembly extends ComponentType("ComponentMessage")

  /**
   * Represents a component that controls one or more assemblies or sequencers
   */
  case object Sequencer extends ComponentType("ComponentMessage")

  /**
   * Represents a general purpose service component e.g. actor and/or web service application
   */
  case object Service extends ComponentType("")
}
