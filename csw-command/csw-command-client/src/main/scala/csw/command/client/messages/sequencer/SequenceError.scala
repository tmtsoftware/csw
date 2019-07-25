package csw.command.client.messages.sequencer

import csw.serializable.CommandSerializable

sealed trait SequenceError extends CommandSerializable

object SequenceError {
  case object DuplicateIdsFound           extends SequenceError
  case object ExistingSequenceIsInProcess extends SequenceError
}
