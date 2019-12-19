package csw.command.client.messages.sequencer

import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.serializable.CommandSerializable

trait SequencerMsg

object SequencerMsg {

  final case class SubmitSequence(sequence: Sequence, replyTo: ActorRef[SubmitResponse])
      extends SequencerMsg
      with CommandSerializable

  final case class Query(runId: Id, replyTo: ActorRef[SubmitResponse]) extends SequencerMsg with CommandSerializable

  final case class QueryFinal(runId: Id, replyTo: ActorRef[SubmitResponse]) extends SequencerMsg with CommandSerializable
}
