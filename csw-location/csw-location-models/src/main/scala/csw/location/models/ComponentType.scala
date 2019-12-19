package csw.location.models

import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represents a type of the Component. It should be serializable since it has to be transmittable over the network.
 * The type will always be represented in lower case.
 *
 * @param messageManifest represents the class name of message that a component will understand
 */
sealed abstract class ComponentType(val messageManifest: String) extends EnumEntry with Snakecase {

  /**
   * The name of ComponentType e.g. for HCD components, the name will be represented as `hcd`.
   */
  def name: String = entryName
}

object ComponentType extends Enum[ComponentType] {

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
  case object Sequencer extends ComponentType("SequencerMsg")

  /**
   * Represents a sequence component e.g ocs_1, iris_1
   */
  case object SequenceComponent extends ComponentType("SequenceComponentMsg")

  /**
   * Represents a general purpose service component e.g. actor and/or web service application
   */
  case object Service extends ComponentType("")
}
