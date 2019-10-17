package csw.command.client

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.client.extensions.BehaviourExtensions
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse.{Completed, Error, QueryResponse, SubmitResponse}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
 *  Contains the Completer class and data types
 */
object CompleterActor {

  trait OverallResponse {
    def responses: Set[QueryResponse]
  }
  case class OverallSuccess(responses: Set[QueryResponse]) extends OverallResponse
  case class OverallFailure(responses: Set[QueryResponse]) extends OverallResponse

  import CommandCompleterMessage._

  sealed trait CommandCompleterMessage
  object CommandCompleterMessage {
    case class Update(sr: QueryResponse)                        extends CommandCompleterMessage
    case class WaitComplete(replyTo: ActorRef[OverallResponse]) extends CommandCompleterMessage
    case object Kill                                            extends CommandCompleterMessage
  }

  private def behavior(
      maybeParentId: Option[Id],
      maybeParentCommand: Option[ControlCommand],
      childResponses: Set[Future[SubmitResponse]],
      loggerFactory: LoggerFactory, // logger could be created inside?
      maybeCrm: Option[CommandResponseManager]
  ): Behavior[CommandCompleterMessage] =
    BehaviourExtensions.withSafeEc[CommandCompleterMessage] { implicit safeEc =>
      {
        var data         = Map[Id, QueryResponse]()
        var failureCount = 0

        childResponses.foreach(_.onComplete {
          case Success(response) => data += response.runId -> response
          case Failure(_)        => failureCount += 1
        })

        val completePromise = Promise[OverallResponse]()
        val log             = loggerFactory.getLogger
        import log._

        checkAndComplete()

        def update(response: QueryResponse): Unit =
          if (data.contains(response.runId)) {
            if (CommandResponse.isIntermediate(data(response.runId))) {
              data += response.runId -> response
              checkAndComplete()
            } else {
              warn(
                "An attempt to update a finished command was detected and was ignored",
                Map("runId" -> response.runId, "existingResponse" -> data(response.runId), "newResponse" -> response)
              )
            }
          } else {
            warn("An attempt to update a non-existing command was detected and ignored", Map("runId" -> response.runId))
          }

        def isAnyResponseNegative: Boolean =
          data.exists { case (_, res) => CommandResponse.isNegative(res) } || failureCount > 0

        def areAllResponsesFinal: Boolean =
          data.forall { case (_, res) => CommandResponse.isFinal(res) } && ((data.size + failureCount) == childResponses.size)

        def checkAndComplete(): Unit =
          if (areAllResponsesFinal) {
            if (isAnyResponseNegative) {
              maybeCrm.foreach(
                _.updateCommand(Error(maybeParentCommand.get.commandName, maybeParentId.get, "Downstream failed"))
              )
              completePromise.trySuccess(OverallFailure(data.values.toSet))
            } else {
              maybeCrm.foreach(_.updateCommand(Completed(maybeParentCommand.get.commandName, maybeParentId.get)))
              completePromise.trySuccess(OverallSuccess(data.values.toSet))
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

  def make(responses: Set[Future[SubmitResponse]], loggerFactory: LoggerFactory)(
      implicit ctx: ActorContext[_]
  ): ActorRef[CommandCompleterMessage] =
    ctx.spawnAnonymous(behavior(None, None, responses, loggerFactory, None))

  def withAutoCompletion(
      parentId: Id,
      parentCommand: ControlCommand,
      childResponses: Set[Future[SubmitResponse]],
      loggerFactory: LoggerFactory,
      crm: CommandResponseManager
  )(implicit ctx: ActorContext[_]): ActorRef[CommandCompleterMessage] =
    ctx.spawnAnonymous(behavior(Some(parentId), Some(parentCommand), childResponses, loggerFactory, Some(crm)))

}
