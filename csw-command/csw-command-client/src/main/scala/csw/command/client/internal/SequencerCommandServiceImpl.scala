package csw.command.client.internal

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.Scheduler
import akka.util.Timeout
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.{ProcessSequence, SequencerMsg}
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{ProcessSequenceError, Sequence}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequencerCommandServiceImpl(sequencerLocation: AkkaLocation)(
    implicit system: ActorSystem[_]
) extends SequencerCommandService {

  private implicit val timeout: Timeout     = Timeout(10.hour)
  private implicit val scheduler: Scheduler = system.scheduler

  private val sequencer: ActorRef[SequencerMsg] = sequencerLocation.sequencerRef

  override def submit(sequence: Sequence): Future[Either[ProcessSequenceError, SubmitResponse]] =
    sequencer ? (ProcessSequence(sequence, _))

}
