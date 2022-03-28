/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.api.utils.SequencerCommandServiceExtension
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.internal.Timeouts
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{Query, QueryFinal, SubmitSequence}
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id

import scala.concurrent.Future

/**
 * Create a SequencerCommandService for sending commands to sequencer
 * @param sequencerLocation  the destination sequencer location to which sequence needs to be sent
 * @param actorSystem required for sending sequence commands or querying the sequencer
 */
class SequencerCommandServiceImpl(sequencerLocation: AkkaLocation)(implicit
    actorSystem: ActorSystem[_]
) extends SequencerCommandService {
  import actorSystem.executionContext

  private val extensions = new SequencerCommandServiceExtension(this)

  private implicit val timeout: Timeout         = Timeouts.DefaultTimeout
  private val sequencer: ActorRef[SequencerMsg] = sequencerLocation.sequencerRef

  override def submit(sequence: Sequence): Future[SubmitResponse] = sequencer ? (SubmitSequence(sequence, _))

  override def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse] =
    extensions.submitAndWait(sequence)

  override def query(runId: Id): Future[SubmitResponse] = sequencer ? (Query(runId, _))

  override def queryFinal(runId: Id)(implicit timeout: Timeout): Future[SubmitResponse] = sequencer ? (QueryFinal(runId, _))
}
