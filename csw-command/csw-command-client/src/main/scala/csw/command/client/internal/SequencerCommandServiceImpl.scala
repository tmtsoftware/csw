package csw.command.client.internal

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages
import csw.command.client.messages.sequencer.LoadAndStartSequence
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequencerCommandServiceImpl(sequencerLocation: AkkaLocation)(
    implicit system: ActorSystem[_]
) extends SequencerCommandService {
  private implicit val timeout: Timeout     = Timeout(10.hour)
  private implicit val scheduler: Scheduler = system.scheduler

  private val sequencer: ActorRef[LoadAndStartSequence] = sequencerLocation.sequencerRef

  override def submit(sequence: Sequence): Future[SubmitResponse] =
    sequencer ? (messages.sequencer.LoadAndStartSequence(sequence, _))
}
