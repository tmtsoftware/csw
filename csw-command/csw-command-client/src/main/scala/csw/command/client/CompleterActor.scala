package csw.command.client

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.client.CompleterActor.CommandCompleterMessage.{Kill, Update, WaitComplete}
import csw.command.client.extensions.BehaviourExtensions
import csw.params.commands.CommandResponse.{Completed, Error, QueryResponse, SubmitResponse}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
 *  Contains the Completer class and data types
 */
object CompleterActor {

  def make(responses: Set[Future[SubmitResponse]])(
      implicit ctx: ActorContext[_]
  ): ActorRef[CommandCompleterMessage] =
    ctx.spawnAnonymous(behavior(responses, None, None, None))

  private def behavior(
      childResponsesF: Set[Future[SubmitResponse]],
      maybeParentId: Option[Id],
      maybeParentCommand: Option[ControlCommand],
      maybeCrm: Option[CommandResponseManager]
  ): Behavior[CommandCompleterMessage] =
    BehaviourExtensions.withSafeEc[CommandCompleterMessage] { implicit safeEc =>
      {
        var childResponses = Map[Id, QueryResponse]()
        var failureCount   = 0

        childResponsesF.foreach(_.onComplete {
          case Success(response) => childResponses += response.runId -> response
          case Failure(_)        => failureCount += 1
        })

        val completePromise = Promise[OverallResponse]()
        val log             = CompleterLogger.getLogger
        import log._

        checkAndComplete()

        def update(response: QueryResponse): Unit =
          if (childResponses.contains(response.runId)) {
            if (CommandResponse.isIntermediate(childResponses(response.runId))) {
              childResponses += response.runId -> response
              checkAndComplete()
            } else {
              warn(
                "An attempt to update a finished command was detected and was ignored",
                Map("runId" -> response.runId, "existingResponse" -> childResponses(response.runId), "newResponse" -> response)
              )
            }
          } else {
            warn("An attempt to update a non-existing command was detected and ignored", Map("runId" -> response.runId))
          }

        def isAnyResponseNegative: Boolean =
          childResponses.exists { case (_, res) => CommandResponse.isNegative(res) } || failureCount > 0

        def areAllResponsesFinal: Boolean =
          childResponses.forall { case (_, res) => CommandResponse.isFinal(res) } && ((childResponses.size + failureCount) == childResponsesF.size)

        def checkAndComplete(): Unit =
          if (areAllResponsesFinal) {
            if (isAnyResponseNegative) {
              maybeCrm.foreach(
                _.updateCommand(Error(maybeParentCommand.get.commandName, maybeParentId.get, "Downstream failed"))
              )
              completePromise.trySuccess(OverallFailure(childResponses.values.toSet))
            } else {
              maybeCrm.foreach(_.updateCommand(Completed(maybeParentCommand.get.commandName, maybeParentId.get)))
              completePromise.trySuccess(OverallSuccess(childResponses.values.toSet))
            }
          }

        Behaviors.receive[CommandCompleterMessage] { (ctx, msg) =>
          msg match {
            case Update(response) =>
              update(response)
              Behaviors.same
            case WaitComplete(replyTo) =>
              completePromise.future.foreach { overallResponse =>
                replyTo ! overallResponse
                ctx.self ! Kill
              }
              Behaviors.same
            case Kill => Behaviors.stopped
          }
        }
      }
    }

  def withAutoCompletion(
      childResponses: Set[Future[SubmitResponse]],
      parentId: Id,
      parentCommand: ControlCommand,
      crm: CommandResponseManager
  )(implicit ctx: ActorContext[_]): ActorRef[CommandCompleterMessage] =
    ctx.spawnAnonymous(behavior(childResponses, Some(parentId), Some(parentCommand), Some(crm)))

  sealed trait CommandCompleterMessage

  trait OverallResponse {
    def responses: Set[QueryResponse]
  }

  case class OverallSuccess(responses: Set[QueryResponse]) extends OverallResponse

  case class OverallFailure(responses: Set[QueryResponse]) extends OverallResponse

  private[command] object CommandCompleterMessage {
    case class Update(sr: QueryResponse)                        extends CommandCompleterMessage
    case class WaitComplete(replyTo: ActorRef[OverallResponse]) extends CommandCompleterMessage
    case object Kill                                            extends CommandCompleterMessage
  }

}
