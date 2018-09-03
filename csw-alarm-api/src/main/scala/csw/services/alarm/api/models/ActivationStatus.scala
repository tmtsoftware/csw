package csw.services.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represents the activation status of an alarm. The activation status of an alarm is configured in the alarm config file.
 * The activation status of an alarm does not change during it's lifespan.
 */
sealed abstract class ActivationStatus extends EnumEntry with Lowercase {

  /**
   * The name of ActivationStatus e.g. for `Active` status, the name will be represented as `active`
   */
  def name: String = entryName
}

object ActivationStatus extends Enum[ActivationStatus] {

  /**
   * Returns a collection of `ActivationStatus` e.g active and inactive
   */
  def values: IndexedSeq[ActivationStatus] = findValues

  /**
   * Represents inactive state of an alarm. Alarms that are `Inactive` could still be updated by setting the severity and
   * could still be read. But they are not considered while aggregating severity and health of a subsystem, component or
   * whole system.
   */
  case object Inactive extends ActivationStatus

  /**
   * Represents active state of the alarm. Active alarms are considered while aggregating severity and health of a subsystem,
   * component or whole system.
   */
  case object Active extends ActivationStatus
}
