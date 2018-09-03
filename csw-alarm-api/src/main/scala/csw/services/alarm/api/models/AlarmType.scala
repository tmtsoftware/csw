package csw.services.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represents a type of the Alarm. Alarm type of an alarm if configured in the alarm config file.
 * Alarm type will not change in entire life span of an alarm
 */
sealed abstract class AlarmType extends EnumEntry with Lowercase {

  /**
   * The name of AlarmType e.g. for Absolute type of alarms, the name will be represented as `absolute`
   */
  def name: String = entryName
}

object AlarmType extends Enum[AlarmType] {

  /**
   * Returns a sequence of all alarm types
   */
  def values: IndexedSeq[AlarmType] = findValues

  case object Absolute     extends AlarmType
  case object BitPattern   extends AlarmType
  case object Calculated   extends AlarmType
  case object Deviation    extends AlarmType
  case object Discrepancy  extends AlarmType
  case object Instrument   extends AlarmType
  case object RateChange   extends AlarmType
  case object RecipeDriven extends AlarmType
  case object Safety       extends AlarmType
  case object Statistical  extends AlarmType
  case object System       extends AlarmType
}
