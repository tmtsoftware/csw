package csw.command.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.CommandResponseManagerActor.{CRMMessage, CRMState}
import csw.command.client.CommandResponseManagerActor.CRMMessage._
import csw.params.commands.CommandResponse.{QueryResponse, SubmitResponse}
import csw.params.core.models.Id

import scala.concurrent.Future

/**
 * Wrapper API for interacting with Command Response Manager of a component
 */
class CommandResponseManager(val commandResponseManagerActor: ActorRef[CRMMessage])(implicit val actorSystem: ActorSystem[_]) {

  private[csw] def addCommand(submitResponse: SubmitResponse): Unit = commandResponseManagerActor ! AddResponse(submitResponse)

  //fixme: should be renamed to updateResponse?
  /**
   * Add a new command or update an existing command with the provided status
   *
   * @param submitResponse final update for a started command [[csw.params.commands.CommandResponse.SubmitResponse]]
   */
  def updateCommand(submitResponse: SubmitResponse): Unit = commandResponseManagerActor ! UpdateResponse(submitResponse)

  def query(runId: Id)(implicit timeout: Timeout): Future[QueryResponse] =
    commandResponseManagerActor ? (Query(runId, _))

  def queryFinal(runId: Id)(implicit timeout: Timeout): Future[QueryResponse] =
    commandResponseManagerActor ? (QueryFinal(runId, _))

  private[client] def getState(implicit timeout: Timeout): Future[Map[Id, CRMState]] = commandResponseManagerActor ? GetState
}
