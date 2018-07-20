package csw.services.alarm.api.models

import csw.messages.TMTSerializable
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class LatchStatus extends EnumEntry with Lowercase with TMTSerializable {

  /**
   * The name of LatchStatus e.g. for Latched status, the name will be represented as `latched`
   */
  def name: String = entryName
}

object LatchStatus extends Enum[LatchStatus] {

  /**
   * Returns a sequence of all alarm types
   */
  def values: IndexedSeq[LatchStatus] = findValues

  case object UnLatched extends LatchStatus
  case object Latched   extends LatchStatus
}
