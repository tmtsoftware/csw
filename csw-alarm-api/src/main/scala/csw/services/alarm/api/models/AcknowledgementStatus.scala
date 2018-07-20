package csw.services.alarm.api.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed abstract class AcknowledgementStatus extends EnumEntry with Lowercase {

  /**
   * The name of AcknowledgementStatus e.g. for `Acknowledged` status, the name will be represented as `acknowledged`
   */
  def name: String = entryName
}

object AcknowledgementStatus extends Enum[AcknowledgementStatus] {

  /**
   * Returns a sequence of all alarm types
   */
  def values: IndexedSeq[AcknowledgementStatus] = findValues

  case object UnAcknowledged extends AcknowledgementStatus
  case object Acknowledged   extends AcknowledgementStatus
}
