package csw.command.client.internal

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{QueryFinal, SubmitSequenceAndWait}
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequencerCommandServiceImpl(sequencerLocation: AkkaLocation)(
    implicit system: ActorSystem[_]
) extends SequencerCommandService {
  private implicit val timeout: Timeout = Timeout(10.hour)

  private val sequencer: ActorRef[SequencerMsg] = sequencerLocation.sequencerRef

  override def submitAndWait(sequence: Sequence): Future[SubmitResponse] =
    sequencer ? (SubmitSequenceAndWait(sequence, _))

  override def queryFinal(runId: Id): Future[SubmitResponse] = sequencer ? (QueryFinal(runId, _))
}
