package csw.command.client.internal

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ProcessSequence
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse.{Error, SubmitResponse}
import csw.params.commands.ProcessSequenceError.{DuplicateIdsFound, ExistingSequenceIsInProcess}
import csw.params.commands.{ProcessSequenceError, Sequence}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequencerCommandServiceImpl(sequencerLocation: AkkaLocation)(
    implicit system: ActorSystem[_]
) extends SequencerCommandService {
  import system.executionContext
  private implicit val timeout: Timeout     = Timeout(10.hour)
  private implicit val scheduler: Scheduler = system.scheduler

  private val sequencer: ActorRef[ProcessSequence] = sequencerLocation.sequencerRef

  override def submit(sequence: Sequence): Future[SubmitResponse] = async {
    val processResponseF: Future[Either[ProcessSequenceError, SubmitResponse]] = sequencer ? (ProcessSequence(sequence, _))
    await(processResponseF) match {
      case Right(submitResponse) => submitResponse
      case Left(error) =>
        error match {
          case DuplicateIdsFound           => Error(sequence.runId, "Duplicate command Id's found in given sequence")
          case ExistingSequenceIsInProcess => Error(sequence.runId, "Submit failed, existing sequence is already in progress")
        }
    }
  }
}
