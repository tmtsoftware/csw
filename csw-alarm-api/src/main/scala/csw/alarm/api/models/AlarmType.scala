package csw.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represents the category of the Alarm. The type of an alarm is configured in the alarm config file which will be fixed
 * for it's entire life span.
 */
sealed abstract class AlarmType extends EnumEntry with Lowercase {

  /**
   * The name of the AlarmType e.g. for `Absolute` type of alarms, the name will be represented as `absolute`
   */
  def name: String = entryName
}

object AlarmType extends Enum[AlarmType] {

  /**
   * Returns a sequence of all alarm types e.g. Absolute, BitPattern, Calculated etc.
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
