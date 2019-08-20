package csw.command.api

import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.SubmitResponse

import scala.concurrent.{Future, Promise}

object CommandCompleter {

  trait OverallResponse {
    def responses: Set[SubmitResponse]
  }
  case class OverallSuccess(responses: Set[SubmitResponse]) extends OverallResponse
  case class OverallFailure(responses: Set[SubmitResponse]) extends OverallResponse

  case class Completer(expectedResponses: Set[SubmitResponse]) {
    private val alreadyCompleted: Set[SubmitResponse] = expectedResponses.filter(CommandResponse.isFinal(_))
    private val startedRunIds = expectedResponses.filter(CommandResponse.isIntermediate(_)).map(_.runId)
    // This accumulates responses until all are gathered
    private var responses = Set.empty[SubmitResponse]
    private val completePromise = Promise[OverallResponse]()

    // Catch the case where one of the startedResponses is a negative
    // Or all the startedResponses are already completed
    if (alreadyCompleted.exists(CommandResponse.isNegative(_)) || alreadyCompleted == expectedResponses) {
      checkAndComplete(expectedResponses)
    }

    // This looks through a set of SubmitResponse and determines if it a an overall success or failure
    private def checkAndComplete(s: Set[SubmitResponse]): Unit = {
      if (!s.exists(CommandResponse.isNegative(_)))
        completePromise.success(OverallSuccess(s))
      else
        completePromise.success(OverallFailure(s))
    }

    /**
     * Called to update the completer with a final result of a long running command
     * @param sr the [[SubmitResponse]] of the completed command
     */
    def update(sr: SubmitResponse): Unit = {
      // Only accept an update from an expected runId is reason for second test
      if (startedRunIds.contains(sr.runId) && !responses.exists(_.runId == sr.runId)) {
        responses += sr
        if (startedRunIds == responses.map(_.runId)) {
          checkAndComplete(alreadyCompleted ++ responses)
        }
      }
    }

    /**
     * Called by client code to wait for all long-running commands to complete
     * @return An [[OverallResponse]] indicating the success or failure of the completed commands
     */
    def waitComplete(): Future[OverallResponse] = {
      completePromise.future
    }
  }

}
