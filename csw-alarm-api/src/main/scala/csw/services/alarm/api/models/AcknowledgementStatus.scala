package csw.services.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represents whether the alarm is acknowledged or not. Whenever the severity of an alarm changes (other than Okay),
 * alarm service changes the status to `Unacknowledged`. Operator is then required to acknowledge the alarm.
 *
 * @note By default all the alarms are loaded in alarm store with `Acknowledged` status
 */
sealed abstract class AcknowledgementStatus extends EnumEntry with Lowercase {

  /**
   * The name of AcknowledgementStatus e.g. for `Acknowledged` status, the name will be represented as `acknowledged`
   */
  def name: String = entryName
}

object AcknowledgementStatus extends Enum[AcknowledgementStatus] {

  /**
   * Returns the collection of `AcknowledgementStatus` e.g. acknowledged and unacknowledged
   */
  def values: IndexedSeq[AcknowledgementStatus] = findValues

  /**
   * Represents unacknowledged status of an alarm. Expects the operator to respond and acknowledge the alarm which will then
   * change the status to Acknowledged.
   */
  case object Unacknowledged extends AcknowledgementStatus

  /**
   * Represents the acknowledged status of the alarm
   */
  case object Acknowledged extends AcknowledgementStatus
}
