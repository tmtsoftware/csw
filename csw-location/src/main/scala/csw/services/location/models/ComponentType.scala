package csw.services.location.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
  * Represents different types of Component. `ComponentType` is serializable as it will be replicated on cluster
  */
sealed abstract class ComponentType extends EnumEntry with Lowercase with TmtSerializable {

  /**
    * Lowercase name of the `ComponentType` e.g. for HCD type of components, the name will be `hcd`
    */
  def name: String = entryName
}

object ComponentType extends Enum[ComponentType] {

  /**
    * Return all `ComponentType` values
    */
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
    * A general purpose service component e.g. actor and/or web service application
    */
  case object Service extends ComponentType

}
