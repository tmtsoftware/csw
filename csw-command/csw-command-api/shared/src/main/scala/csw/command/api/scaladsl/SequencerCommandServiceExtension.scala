package csw.command.api.scaladsl

import akka.util.Timeout
import csw.params.commands.CommandResponse._
import csw.params.commands.Sequence

import scala.concurrent.{ExecutionContext, Future}

class SequencerCommandServiceExtension(
    sequencerCommandService: SequencerCommandService
)(implicit ec: ExecutionContext) {

  def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse] = {
    sequencerCommandService.submit(sequence).flatMap {
      case Started(runId) => sequencerCommandService.queryFinal(runId)
      case x              => Future.successful(x)
    }
  }

}
