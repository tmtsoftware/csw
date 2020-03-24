package csw.command.client

import java.util.concurrent.CompletableFuture

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.CommandResponseManager.{OverallFailure, OverallResponse, OverallSuccess}
import csw.command.client.MiniCRM.MiniCRMMessage.AddResponse
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.SubmitResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

/**
 * Wrapper API for interacting with Command Response Manager of a component
 *
 * @param commandResponseManagerActor underlying actor managing command responses for started commands
 * @param actorSystem actor system for allowing sending messages in API
 */
class CommandResponseManager(val commandResponseManagerActor: ActorRef[MiniCRM.CRMMessage])(
    implicit val actorSystem: ActorSystem[_]
) {

  /**
   * Add a new command or update an existing command with the provided status
   *
   * @param submitResponse final update for a started command [[csw.params.commands.CommandResponse.SubmitResponse]]
   */
  def updateCommand(submitResponse: SubmitResponse): Unit = commandResponseManagerActor ! AddResponse(submitResponse)

  implicit val ex: ExecutionContext = actorSystem.executionContext

  /**
   * queryFinal allows executing code when all the provided commands have completed.
   *
   * @param commands commands that have been started with submit or submitAndWait
   * @return An overall response indicated success or failure
   */
  def queryFinalAll(commands: Future[SubmitResponse]*): Future[OverallResponse] =
    Future.sequence(commands.toSet).map { s =>
      if (isSuccessful(s)) {
        OverallSuccess(s)
      }
      else {
        OverallFailure(s)
      }
    }

  /**
   * Java API: queryFinal allows executing code when all the provided commands have completed.
   *
   * @param commands commands that have been started with submit or submitAndWait
   * @return An overall response indicated success or failure
   */
  def queryFinalAll(commands: java.util.List[CompletableFuture[SubmitResponse]]): CompletableFuture[OverallResponse] = {
    val args = commands.asScala.toList.map(_.asScala)
    queryFinalAll(args: _*).asJava.toCompletableFuture
  }

  // Returns true if all the commands in the response set have returned without Error
  private def isSuccessful(responses: Set[SubmitResponse]): Boolean = {
    !responses.exists(CommandResponse.isNegative)
  }
}

object CommandResponseManager {

  trait OverallResponse {
    def responses: Set[SubmitResponse]

    /**
     * Java API to get the set of responses
     */
    def getResponses: java.util.Set[SubmitResponse] = responses.asJava
  }

  /**
   * Indicates that all responses included completed successfully.
   * @param responses the set of responses
   */
  case class OverallSuccess(responses: Set[SubmitResponse]) extends OverallResponse

  /**
   * Indicates that at least one of the responses ended with a negative response
   * @param responses the set of responses
   */
  case class OverallFailure(responses: Set[SubmitResponse]) extends OverallResponse

}
