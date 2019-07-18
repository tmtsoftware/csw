package csw.command.client.messages

import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{ProcessSequenceError, Sequence}
import csw.serializable.CommandSerializable

sealed trait SequencerMsg
sealed trait ExternalSequencerMsg extends SequencerMsg with CommandSerializable
final case class ProcessSequence(sequence: Sequence, replyTo: ActorRef[Either[ProcessSequenceError, SubmitResponse]])
    extends ExternalSequencerMsg
