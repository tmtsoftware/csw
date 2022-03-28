/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.messages.sequencer

import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.serializable.CommandSerializable

trait SequencerMsg

sealed trait CswSequencerMessage extends SequencerMsg with CommandSerializable

object SequencerMsg {
  final case class SubmitSequence(sequence: Sequence, replyTo: ActorRef[SubmitResponse]) extends CswSequencerMessage
  final case class Query(runId: Id, replyTo: ActorRef[SubmitResponse])                   extends CswSequencerMessage
  final case class QueryFinal(runId: Id, replyTo: ActorRef[SubmitResponse])              extends CswSequencerMessage
}
