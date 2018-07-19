package csw.services.alarm.api.models

import csw.messages.TMTSerializable
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import upickle.default.{macroRW, ReadWriter ⇒ RW}

import scala.collection.immutable.IndexedSeq

sealed abstract class AcknowledgementStatus extends EnumEntry with Lowercase with TMTSerializable {

  /**
   * The name of AcknowledgementStatus e.g. for `Acknowledged` status, the name will be represented as `acknowledged`
   */
  def name: String = entryName
}

//TODO: remove play-json
//TODO: use enum helper for upickle
//TODO: use a single file for all pickles
object AcknowledgementStatus extends Enum[AcknowledgementStatus] with PlayJsonEnum[AcknowledgementStatus] {

  /**
   * Returns a sequence of all alarm types
   */
  def values: IndexedSeq[AcknowledgementStatus] = findValues

  case object UnAcknowledged extends AcknowledgementStatus
  case object Acknowledged   extends AcknowledgementStatus

  implicit def acknowledgementStatusRw: RW[AcknowledgementStatus] = RW.merge(
    macroRW[AcknowledgementStatus.UnAcknowledged.type],
    macroRW[AcknowledgementStatus.Acknowledged.type]
  )
}
