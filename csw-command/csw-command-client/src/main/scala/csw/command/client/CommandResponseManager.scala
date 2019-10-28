package csw.command.client

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.CommandResponseManagerActor.CRMMessage
import csw.command.client.CommandResponseManagerActor.CRMMessage.{AddResponse, UpdateResponse}
import csw.params.commands.CommandResponse.{QueryResponse, SubmitResponse}
import csw.params.core.models.Id
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.Future

/**
 * Wrapper API for interacting with Command Response Manager of a component
 */
class CommandResponseManager(val commandResponseManagerActor: ActorRef[CRMMessage])(implicit val actorSystem: ActorSystem[_]) {

  /**
   * Add a new command or update an existing command with the provided status
   *
   * @param submitResponse final update for a started command [[csw.params.commands.CommandResponse.SubmitResponse]]
   */
  def updateCommand(submitResponse: SubmitResponse): Unit = commandResponseManagerActor ! UpdateResponse(submitResponse)

  def queryFinal(runId: Id)(implicit timeout: Timeout): Future[QueryResponse] =
    commandResponseManagerActor ? (CRMMessage.QueryFinal(runId, _))

  private[csw] def addCommand(submitResponse: SubmitResponse): Unit = commandResponseManagerActor ! AddResponse(submitResponse)
}
