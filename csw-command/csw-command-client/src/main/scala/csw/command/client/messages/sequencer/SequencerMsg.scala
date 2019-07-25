package csw.command.client.messages.sequencer

import akka.actor.typed.ActorRef
import csw.params.commands.Sequence
import csw.serializable.CommandSerializable

trait SequencerMsg
final case class LoadAndStartSequence(sequence: Sequence, replyTo: ActorRef[SequenceResponse])
    extends SequencerMsg
    with CommandSerializable
