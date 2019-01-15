package csw.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represents whether the alarm is shelved or not. Alarms that are shelved will automatically be unshelved at a specific
 * time (currently configured at 8 AM local time) if it is not unshelved explicitly. This time is configurable
 * e.g csw-alarm.shelve-timeout = h:m:s a .
 *
 * @note By default all the alarms are loaded in alarm store with `Unshelved` status
 */
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

  /**
   * Represents the normal(unshelved) status of an alarm. By default all the alarms are loaded in alarm store with `Unshelved`
   * status.
   *
   * @note if a shelved alarm is not explicitly unshelved then it will automatically be unshelved at a specific time i.e.
   *       currently 8 AM local time. This time is configurable e.g csw-alarm.shelve-timeout = h:m:s a .
   */
  case object Unshelved extends ShelveStatus

  /**
   * Represents the shelved status of an alarm. Once alarm is shelved no response would be needed in terms of acknowledgement,
   * reset, etc.
   *
   * @note Shelved alarms are considered in aggregation of health and severity
   */
  case object Shelved extends ShelveStatus
}
