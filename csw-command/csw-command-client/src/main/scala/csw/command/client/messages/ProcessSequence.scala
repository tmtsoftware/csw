package csw.command.client.messages

import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.serializable.CommandSerializable

trait SequencerMsg
final case class ProcessSequence(sequence: Sequence, replyTo: ActorRef[ProcessSequenceResponse])
    extends SequencerMsg
    with CommandSerializable

sealed trait ProcessSequenceError extends CommandSerializable

object ProcessSequenceError {
  case object DuplicateIdsFound           extends ProcessSequenceError
  case object ExistingSequenceIsInProcess extends ProcessSequenceError
}

case class ProcessSequenceResponse(response: Either[ProcessSequenceError, SubmitResponse]) extends CommandSerializable
