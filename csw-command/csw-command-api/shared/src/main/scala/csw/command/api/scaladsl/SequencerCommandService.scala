package csw.command.api.scaladsl

import akka.util.Timeout
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id

import scala.concurrent.Future

/**
 * A command Service API of a sequencer. This model provides method based APIs for command interactions with a sequencer.
 */
trait SequencerCommandService {
  def submit(sequence: Sequence): Future[SubmitResponse]
  def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse]
  def query(runId: Id): Future[SubmitResponse]
  def queryFinal(runId: Id)(implicit timeout: Timeout): Future[SubmitResponse]
}
