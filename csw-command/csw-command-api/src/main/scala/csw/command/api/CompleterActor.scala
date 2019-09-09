package csw.command.api

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.api.Completer.{OverallFailure, OverallResponse, OverallSuccess}
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.SubmitResponse

/**
 * Contains the CompleterActor and its messages
 */
object CompleterActor {
  import CommandCompleterMessage._

  sealed trait CommandCompleterMessage
  object CommandCompleterMessage {
    case class Update(sr: SubmitResponse)                       extends CommandCompleterMessage
    case class WaitComplete(replyTo: ActorRef[OverallResponse]) extends CommandCompleterMessage
  }

  private def make(expectedResponses: Set[SubmitResponse]): Behavior[CommandCompleterMessage] =
    Behaviors.setup { _ =>
      // alreadyCompleted is the set of commands that are already completed when handed to the actor
      val alreadyCompleted: Set[SubmitResponse] = expectedResponses.filter(CommandResponse.isFinal(_))
      // started is the set of commands that are started/long-running
      val started: Set[SubmitResponse]          = expectedResponses.filter(CommandResponse.isIntermediate(_))

      // This accumulates updates from started commands until all are gathered
      val updates = Set.empty[SubmitResponse]

      handle(alreadyCompleted, started, updates)
    }

  private def handle(
      alreadyCompleted: Set[SubmitResponse],
      started: Set[SubmitResponse],
      updates: Set[SubmitResponse]
  ): Behavior[CommandCompleterMessage] = Behaviors.receive { (_, message) =>
    message match {
      case Update(sr: SubmitResponse) =>
        if (started.map(_.runId).contains(sr.runId) && !updates.exists(_.runId == sr.runId)) {
          handle(alreadyCompleted, started, updates + sr)
        } else
          handle(alreadyCompleted, started, updates)
      case WaitComplete(replyTo: ActorRef[OverallResponse]) =>
        // Catch the case where one of the already completed is a negative resulting in failure
        // Or all the commands are already completed
        if (alreadyCompleted.exists(CommandResponse.isNegative(_)) || started.isEmpty) {
          // started is included here for the first case above. alreadyDone has Error and started.size is non zero
          checkAndComplete(alreadyCompleted ++ started, replyTo)
          Behaviors.stopped
        } else {
          // If all updates received, send reply and exit else wait for updates
          if (allUpdatesReceived(started, updates)) {
            checkAndComplete(alreadyCompleted ++ updates, replyTo)
            Behaviors.stopped
          } else {
            receivedWait(alreadyCompleted, started, updates, replyTo)
          }
        }
    }
  }

  private def receivedWait(
      alreadyCompleted: Set[SubmitResponse],
      started: Set[SubmitResponse],
      updates: Set[SubmitResponse],
      replyTo: ActorRef[OverallResponse]
  ): Behavior[CommandCompleterMessage] = Behaviors.receive { (_, message) =>
    message match {
      case Update(sr: SubmitResponse) =>
        if (started.map(_.runId).contains(sr.runId) && !updates.exists(_.runId == sr.runId)) {
          val newUpdates = updates + sr
          if (allUpdatesReceived(started, newUpdates)) {
            checkAndComplete(alreadyCompleted ++ newUpdates, replyTo)
            Behaviors.stopped
          } else {
            receivedWait(alreadyCompleted, started, newUpdates, replyTo)
          }
        } else
          receivedWait(alreadyCompleted, started, updates, replyTo)
      case WaitComplete(replyTo: ActorRef[OverallResponse]) =>
        receivedWait(alreadyCompleted, started, updates, replyTo)
    }
  }

  private def allUpdatesReceived(started: Set[SubmitResponse], updates: Set[SubmitResponse]): Boolean =
    started.map(_.runId) == updates.map(_.runId)

  // This looks through a set of SubmitResponse and determines if it a an overall success or failure
  private def checkAndComplete(s: Set[SubmitResponse], replyTo: ActorRef[OverallResponse]): Unit =
    if (!s.exists(CommandResponse.isNegative(_)))
      replyTo ! OverallSuccess(s)
    else
      replyTo ! OverallFailure(s)

  def apply(expectedResponses: Set[SubmitResponse]): Behavior[CommandCompleterMessage] = make(expectedResponses)

}
