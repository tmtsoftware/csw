package csw.command.api

import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.SubmitResponse

import scala.concurrent.{Future, Promise}

/**
 *  Contains the Completer class and data types
 */
object Completer {

  trait OverallResponse {
    def responses: Set[SubmitResponse]
  }
  case class OverallSuccess(responses: Set[SubmitResponse]) extends OverallResponse
  case class OverallFailure(responses: Set[SubmitResponse]) extends OverallResponse

  case class Completer(expectedResponses: Set[SubmitResponse]) {
    // alreadyCompleted is the set of commands that are already completed when handed to the actor
    private val alreadyCompleted: Set[SubmitResponse] = expectedResponses.filter(CommandResponse.isFinal(_))
    // started is the set of commands that are started/long-running
    private val started: Set[SubmitResponse] = expectedResponses.filter(CommandResponse.isIntermediate(_))
    // This accumulates responses until all expected are gathered
    private var updates         = Set.empty[SubmitResponse]
    private val completePromise = Promise[OverallResponse]()

    // Catch the case where one of the already completed is a negative resulting in failure already
    // Or all the commands are already completed
    if (alreadyCompleted.exists(CommandResponse.isNegative(_)) || started.isEmpty) {
      checkAndComplete(expectedResponses)
    }

    // This looks through a set of SubmitResponse and determines if it is an overall success or failure
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
    def update(sr: SubmitResponse): Unit = synchronized {
      // Only accept an update from an expected runId is reason for second test
      if (started.map(_.runId).contains(sr.runId) && !updates.exists(_.runId == sr.runId)) {
        updates += sr
        if (started.map(_.runId) == updates.map(_.runId)) {
          checkAndComplete(alreadyCompleted ++ updates)
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
