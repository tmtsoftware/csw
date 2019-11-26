package csw.command.api.scaladsl

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.params.commands.CommandResponse._
import csw.params.commands.Sequence

import scala.concurrent.Future

class SequencerCommandServiceExtension(
    sequencerCommandService: SequencerCommandService
)(implicit val actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse] = {
    sequencerCommandService.submit(sequence).flatMap {
      case Started(runId) => sequencerCommandService.queryFinal(runId)
      case x              => Future.successful(x)
    }
  }

}
