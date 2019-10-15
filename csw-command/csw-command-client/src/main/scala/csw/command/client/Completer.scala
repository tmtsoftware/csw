package csw.command.client

import java.util.concurrent.ConcurrentHashMap

import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse.{Completed, Error, QueryResponse, SubmitResponse}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.convert.ImplicitConversionsToScala.{`iterable AsScalaIterable`, `map AsScalaConcurrentMap`}
import scala.concurrent.{Future, Promise}

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
      childResponses: Set[SubmitResponse],
      loggerFactory: LoggerFactory,
      maybeCrm: Option[CommandResponseManager]
  ) {
    private val data            = new ConcurrentHashMap[Id, QueryResponse](childResponses.map(res => res.runId -> res).toMap.asJava)
    private val completePromise = Promise[OverallResponse]()
    private val log             = loggerFactory.getLogger
    import log._

    // Catch the case where one of the already completed is a negative resulting in failure already
    // Or all the commands are already completed

    private def maybeNegativeResponse: Option[QueryResponse] =
      data.find { case (_, res) => CommandResponse.isNegative(res) }.map(_._2)
    private def areAllResponsesFinal: Boolean = data.forall { case (_, res) => CommandResponse.isFinal(res) }

    checkAndComplete()

    private def checkAndComplete(): Unit = {
      if (areAllResponsesFinal) {
        if (maybeNegativeResponse.isDefined) {
          maybeCrm.foreach(_.updateCommand(Error(maybeParentCommand.get.commandName, maybeParentId.get, "Downstream failed")))
          completePromise.trySuccess(OverallFailure(data.values().toSet))
        } else {
          maybeCrm.foreach(_.updateCommand(Completed(maybeParentCommand.get.commandName, maybeParentId.get)))
          completePromise.trySuccess(OverallSuccess(data.values().toSet))
        }
      }
    }

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
            Map(
              "runId"            -> response.runId,
              "existingResponse" -> data(response.runId),
              "newResponse"      -> response
            )
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

  }

  object Completer {
    def apply(responses: Set[SubmitResponse], loggerFactory: LoggerFactory): Completer =
      new Completer(None, None, responses, loggerFactory, None)

    def withAutoCompletion(
        parentId: Id,
        parentCommand: ControlCommand,
        childResponses: Set[SubmitResponse],
        loggerFactory: LoggerFactory,
        crm: CommandResponseManager
    ) = new Completer(Some(parentId), Some(parentCommand), childResponses, loggerFactory, Some(crm))

  }
}
