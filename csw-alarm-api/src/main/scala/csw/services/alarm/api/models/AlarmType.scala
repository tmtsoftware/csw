package csw.services.alarm.api.models

import csw.messages.TMTSerializable
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable.IndexedSeq

/**
 * Represents a type of the Alarm. It should be serializable since it has to be transmittable over the network.
 * The type will always be represented in lower case.
 */
sealed abstract class AlarmType extends EnumEntry with Lowercase with TMTSerializable {

  /**
   * The name of AlarmType e.g. for Absolute type of alarms, the name will be represented as `absolute`
   */
  def name: String = entryName
}

object AlarmType extends Enum[AlarmType] with PlayJsonEnum[AlarmType] {

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

  import upickle.default.{macroRW, ReadWriter ⇒ RW}

  implicit def alarmTypeRw: RW[AlarmType] = RW.merge(
    macroRW[AlarmType.Absolute.type],
    macroRW[AlarmType.BitPattern.type],
    macroRW[AlarmType.Calculated.type],
    macroRW[AlarmType.Deviation.type],
    macroRW[AlarmType.Discrepancy.type],
    macroRW[AlarmType.Instrument.type],
    macroRW[AlarmType.RateChange.type],
    macroRW[AlarmType.RecipeDriven.type],
    macroRW[AlarmType.Safety.type],
    macroRW[AlarmType.Statistical.type],
    macroRW[AlarmType.System.type]
  )
}
