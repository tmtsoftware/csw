package csw.services.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class ShelveStatus extends EnumEntry with Lowercase {

  /**
   * The name of ShelveStatus e.g. for `Shelved` status, the name will be represented as `shelved`
   */
  def name: String = entryName
}

object ShelveStatus extends Enum[ShelveStatus] {

  /**
   * Returns a collection of `ShelveStatus` e.g shelved or unshelved
   */
  def values: IndexedSeq[ShelveStatus] = findValues

  case object Unshelved extends ShelveStatus
  case object Shelved   extends ShelveStatus
}
