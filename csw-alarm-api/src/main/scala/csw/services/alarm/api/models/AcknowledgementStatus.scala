package csw.services.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represents the acknowledgement related status of the alarm for e.g if it is acknowledged or not. Whenever the severity of an
 * alarm changes (other than Okay), alarm service changes the status to `Unacknowledged` (provided the alarm is not auto-acknowledgable).
 * Operator is then required to acknowledge the alarm which will then set the status to `Acknowledged`.
 *
 * @note By default all the alarms are loaded in alarm store with `Unacknowledged` status
 */
sealed abstract class AcknowledgementStatus extends EnumEntry with Lowercase {

  /**
   * The name of AcknowledgementStatus e.g. for `Acknowledged` status, the name will be represented as `acknowledged`
   */
  def name: String = entryName
}

object AcknowledgementStatus extends Enum[AcknowledgementStatus] {

  /**
   * Returns the collection of values of `AcknowledgementStatus`
   */
  def values: IndexedSeq[AcknowledgementStatus] = findValues

  /**
   * Represents unacknowledged status of an alarm. Expects the operator to respond and acknowledge the alarm which will then
   * change the status to Acknowledged.
   */
  case object Unacknowledged extends AcknowledgementStatus

  /**
   * Represents the acknowledged status of the alarm. Alarms that are auto-acknowledgable will automatically go in `Acknowledged`
   * status whenever the severity changes.
   */
  case object Acknowledged extends AcknowledgementStatus
}
