package csw.services.alarm.api.models

import csw.messages.TMTSerializable
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class ActivationStatus extends EnumEntry with Lowercase with TMTSerializable {

  /**
   * The name of ActivationStatus e.g. for `Active` status, the name will be represented as `active`
   */
  def name: String = entryName
}

object ActivationStatus extends Enum[ActivationStatus] {

  /**
   * Returns a sequence of all alarm types
   */
  def values: IndexedSeq[ActivationStatus] = findValues

  case object Inactive extends ActivationStatus
  case object Active   extends ActivationStatus
}
