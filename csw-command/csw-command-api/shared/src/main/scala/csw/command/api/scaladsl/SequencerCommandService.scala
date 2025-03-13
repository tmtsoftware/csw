/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.api.scaladsl

import org.apache.pekko.util.Timeout
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id

import scala.concurrent.Future

/**
 * A command Service API of a sequencer. This model provides method based APIs for command interactions with a sequencer.
 */
trait SequencerCommandService {

  /**
   * Submit the given sequence to the sequencer.
   * If the sequencer is idle, the provided sequence is loaded in the sequencer and execution of the sequence starts immediately,
   * and a [[csw.params.commands.CommandResponse.Started]] response is returned
   * If the sequencer is already running another sequence, an [[csw.params.commands.CommandResponse.Invalid]] response is returned
   *
   * @param sequence to run on the sequencer
   * @return an initial [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future value
   */
  def submit(sequence: Sequence): Future[SubmitResponse]

  /**
   * Submit the given sequence to the sequencer and wait for the final response if the sequence was successfully [[csw.params.commands.CommandResponse.Started]].
   * If the sequencer is idle, the provided sequence will be [[submit]]ted to the sequencer and the final response will be returned.
   * If the sequencer is already running another sequence, an [[csw.params.commands.CommandResponse.Invalid]] response is returned.
   *
   * @param sequence to run on the sequencer
   * @param timeout max-time to wait for a final response
   * @return a final [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future value
   */
  def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse]

  /**
   * Query for the result of the sequence which was [[submit]]ted to get a [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future.
   * Query allows checking to see if the long-running sequence is completed without waiting as with [[queryFinal]].
   * @param runId of the sequence under execution
   * @return a [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future value
   */
  def query(runId: Id): Future[SubmitResponse]

  /**
   * Query for the final result of a long running sequence which was sent through [[submit]]
   * @param runId of the sequence under execution
   * @param timeout max-time to wait for a final response
   * @return a final [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future value
   */
  def queryFinal(runId: Id)(implicit timeout: Timeout): Future[SubmitResponse]
}
