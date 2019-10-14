package csw.command.api

import java.util
import java.util.concurrent.ConcurrentHashMap

import csw.logging.client.scaladsl.{GenericLoggerFactory, LoggerFactory}
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.{QueryResponse, SubmitResponse}
import csw.params.core.models.Id

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.convert.ImplicitConversionsToScala.{`iterable AsScalaIterable`, `map AsScalaConcurrentMap`, `set asScala`}
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

  class Completer(responses: Set[SubmitResponse], loggerFactory: LoggerFactory) {
    private val data            = new ConcurrentHashMap[Id, QueryResponse](responses.map(res => res.runId -> res).toMap.asJava)
    private val completePromise = Promise[OverallResponse]()
    private val log             = loggerFactory.getLogger
    import log._

    // Catch the case where one of the already completed is a negative resulting in failure already
    // Or all the commands are already completed

    private def isAnyResponseNegative: Boolean = data.exists { case (_, res) => CommandResponse.isNegative(res) }
    private def areAllResponsesFinal: Boolean  = data.forall { case (_, res) => CommandResponse.isFinal(res) }

    checkAndComplete()

    // This looks through all the SubmitResponses and determines if it is an overall success or failure
    private def checkAndComplete(): Unit = {
      if (areAllResponsesFinal) {
        if (isAnyResponseNegative) completePromise.trySuccess(OverallFailure(data.values().toSet))
        else completePromise.trySuccess(OverallSuccess(data.values().toSet))
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
}
