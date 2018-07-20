package csw.services.alarm.api.models

import csw.messages.TMTSerializable
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class ShelveStatus extends EnumEntry with Lowercase with TMTSerializable {

  /**
   * The name of ShelveStatus e.g. for `Shelved` status, the name will be represented as `shelved`
   */
  def name: String = entryName
}

object ShelveStatus extends Enum[ShelveStatus] {

  /**
   * Returns a sequence of all alarm types
   */
  def values: IndexedSeq[ShelveStatus] = findValues

  case object UnShelved extends ShelveStatus
  case object Shelved   extends ShelveStatus
}
