package csw.command.client

import java.util.concurrent.ConcurrentHashMap

import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.{QueryResponse, SubmitResponse}
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
      responses: Set[SubmitResponse],
      loggerFactory: LoggerFactory,
      maybeCrm: Option[CommandResponseManager]
  ) {
    private val data            = new ConcurrentHashMap[Id, QueryResponse](responses.map(res => res.runId -> res).toMap.asJava)
    private val completePromise = Promise[OverallResponse]()
    private val log             = loggerFactory.getLogger
    import log._

    // Catch the case where one of the already completed is a negative resulting in failure already
    // Or all the commands are already completed

    private def maybeNegativeResponse: Option[QueryResponse] =
      data.find { case (_, res) => CommandResponse.isNegative(res) }.map(_._2)
    private def areAllResponsesFinal: Boolean = data.forall { case (_, res) => CommandResponse.isFinal(res) }

    checkAndComplete()

    // This looks through all the SubmitResponses and determines if it is an overall success or failure
    private def checkAndComplete(): Unit = {
      if (areAllResponsesFinal) {
        if (maybeNegativeResponse.isDefined) {
          completePromise.trySuccess(OverallFailure(data.values().toSet))
//          maybeCrm.foreach(
//            crm =>
//            // fixme:
//              crm.updateCommand(maybeNegativeResponse.get match {
//                case response: SubmitResponse => response
//                case CommandResponse.CommandNotAvailable(commandName, runId) =>
//                  Error(commandName, runId, "Downstream failed to process too many commands")
//              })
//          )
        } else {
          completePromise.trySuccess(OverallSuccess(data.values().toSet))
//          maybeCrm.map(crm => crm.updateCommand(Completed()))
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
      new Completer(responses, loggerFactory, None)
    def withAutoCompletion(responses: Set[SubmitResponse], loggerFactory: LoggerFactory, crm: CommandResponseManager) =
      new Completer(responses, loggerFactory, Some(crm))
  }
}
