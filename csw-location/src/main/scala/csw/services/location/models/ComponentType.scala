package csw.services.location.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * CSW Component types
 */
sealed abstract class ComponentType extends EnumEntry with Lowercase {
  def name: String = entryName
}

object ComponentType extends Enum[ComponentType]{

  def values: IndexedSeq[ComponentType] = findValues

  /**
   * A container for components (assemblies and HCDs)
   */
  case object Container extends ComponentType

  /**
   * A component that controls a hardware device
   */
  case object HCD extends ComponentType

  /**
   * A component that controls one or more HCDs or assemblies
   */
  case object Assembly extends ComponentType

  /**
   * A general purpose service component (actor and/or web service application)
   */
  case object Service extends ComponentType
}
