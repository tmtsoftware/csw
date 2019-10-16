package csw.command.client

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse.{Completed, Error, QueryResponse, SubmitResponse}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id

import scala.collection.convert.ImplicitConversionsToScala.{`iterable AsScalaIterable`, `map AsScalaConcurrentMap`}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/**
 *  Contains the Completer class and data types
 */
object Completer {

  trait OverallResponse {
    def responses: Set[QueryResponse]
  }
  case class OverallSuccess(responses: Set[QueryResponse]) extends OverallResponse
  case class OverallFailure(responses: Set[QueryResponse]) extends OverallResponse

  class Completer private (
      maybeParentId: Option[Id],
      maybeParentCommand: Option[ControlCommand],
      childResponses: Set[Future[SubmitResponse]],
      loggerFactory: LoggerFactory,
      maybeCrm: Option[CommandResponseManager]
  )(implicit ec: ExecutionContext) {

    private val data         = new ConcurrentHashMap[Id, QueryResponse]()
    private val failureCount = new AtomicInteger(0)

    childResponses.foreach(_.onComplete {
      case Success(response) => data.put(response.runId, response)
      case Failure(_)        => failureCount.getAndIncrement()
    })

    private val completePromise = Promise[OverallResponse]()
    private val log             = loggerFactory.getLogger
    import log._

    checkAndComplete()

    /**
     * Called to update the completer with a final result of a long running command
     * @param response the [[SubmitResponse]] of the completed command
     */
    def update(response: QueryResponse): Unit = {
      if (data.containsKey(response.runId)) {
        if (CommandResponse.isIntermediate(data(response.runId))) {
          data.put(response.runId, response)
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
    }

    /**
     * Called by client code to wait for all long-running commands to complete
     * @return An [[OverallResponse]] indicating the success or failure of the completed commands
     */
    def waitComplete(): Future[OverallResponse] = {
      completePromise.future
    }

    private def isAnyResponseNegative: Boolean =
      data.exists { case (_, res) => CommandResponse.isNegative(res) } || failureCount.get() > 0

    private def areAllResponsesFinal: Boolean =
      data.forall { case (_, res) => CommandResponse.isFinal(res) } && ((data.size() + failureCount.get()) == childResponses.size)

    private def checkAndComplete(): Unit = {
      if (areAllResponsesFinal) {
        if (isAnyResponseNegative) {
          maybeCrm.foreach(_.updateCommand(Error(maybeParentCommand.get.commandName, maybeParentId.get, "Downstream failed")))
          completePromise.trySuccess(OverallFailure(data.values().toSet))
        } else {
          maybeCrm.foreach(_.updateCommand(Completed(maybeParentCommand.get.commandName, maybeParentId.get)))
          completePromise.trySuccess(OverallSuccess(data.values().toSet))
        }
      }
    }
  }

  object Completer {
    def apply(responses: Set[Future[SubmitResponse]], loggerFactory: LoggerFactory)(implicit ec: ExecutionContext): Completer =
      new Completer(None, None, responses, loggerFactory, None)

    def withAutoCompletion(
        parentId: Id,
        parentCommand: ControlCommand,
        childResponses: Set[Future[SubmitResponse]],
        loggerFactory: LoggerFactory,
        crm: CommandResponseManager
    )(implicit ec: ExecutionContext) =
      new Completer(Some(parentId), Some(parentCommand), childResponses, loggerFactory, Some(crm))

  }
}
