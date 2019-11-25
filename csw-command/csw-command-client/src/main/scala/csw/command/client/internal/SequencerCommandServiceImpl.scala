package csw.command.client.internal

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{Query, QueryFinal, SubmitSequence}
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse.{QueryResponse, Started, SubmitResponse}
import csw.params.commands.Sequence
import csw.params.core.models.Id

import scala.concurrent.Future

class SequencerCommandServiceImpl(sequencerLocation: AkkaLocation)(
    implicit system: ActorSystem[_]
) extends SequencerCommandService {

  private implicit val timeout: Timeout         = Timeouts.DefaultTimeout
  private val sequencer: ActorRef[SequencerMsg] = sequencerLocation.sequencerRef

  override def submit(sequence: Sequence): Future[SubmitResponse] = sequencer ? (SubmitSequence(sequence, _))

  override def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse] = {
    import system.executionContext
    submit(sequence).flatMap {
      case Started(runId) => queryFinal(runId)
      case x              => Future.successful(x)
    }
  }

  override def query(runId: Id): Future[QueryResponse] = sequencer ? (Query(runId, _))

  override def queryFinal(runId: Id)(implicit timeout: Timeout): Future[SubmitResponse] = sequencer ? (QueryFinal(runId, _))
}
