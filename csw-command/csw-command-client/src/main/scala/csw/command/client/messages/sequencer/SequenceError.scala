package csw.command.client.messages.sequencer

import csw.serializable.CommandSerializable
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait SequenceError extends CommandSerializable with EnumEntry

object SequenceError extends Enum[SequenceError] {
  override def values: immutable.IndexedSeq[SequenceError] = findValues
  case object DuplicateIdsFound           extends SequenceError
  case object ExistingSequenceIsInProcess extends SequenceError
}
