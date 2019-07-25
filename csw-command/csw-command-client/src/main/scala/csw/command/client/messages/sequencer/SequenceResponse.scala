package csw.command.client.messages.sequencer

import csw.params.commands.CommandResponse.SubmitResponse
import csw.serializable.CommandSerializable

final case class SequenceResponse(response: Either[SequenceError, SubmitResponse]) extends CommandSerializable
