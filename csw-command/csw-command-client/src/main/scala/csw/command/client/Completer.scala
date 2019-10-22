package csw.command.client

import akka.actor.typed.{ActorRef, Scheduler}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.command.client.CompleterActor.CommandCompleterMessage.{Update, WaitComplete}
import csw.command.client.CompleterActor.{CommandCompleterMessage, OverallResponse}
import csw.params.commands.CommandResponse.{QueryResponse, SubmitResponse}
import csw.params.commands.ControlCommand
import csw.params.core.models.Id

import scala.concurrent.Future

/**
 *  Contains the Completer class and data types
 */
class Completer private (ref: ActorRef[CommandCompleterMessage])(implicit ctx: ActorContext[_]) {

  /**
   * Called to update the completer with a final result of a long running command
   *
   * @param response the [[SubmitResponse]] of the completed command
   */
  // this is incorrect. since initial responses are added to data cache on future completion, this method if not used properly will result
  // into unexpected behavior. e.g. update happens before future completion
  def update(response: QueryResponse): Unit = ref ! Update(response)

  /**
   * Called by client code to wait for all long-running commands to complete
   *
   * @return An [[OverallResponse]] indicating the success or failure of the completed commands
   */
  def waitComplete()(implicit timeout: Timeout): Future[OverallResponse] = {
    implicit val scheduler: Scheduler = ctx.system.scheduler
    ref ? WaitComplete
  }
}

object Completer {
  def apply(responses: Set[Future[SubmitResponse]])(implicit ctx: ActorContext[_]): Completer =
    new Completer(CompleterActor.make(responses))

  def withAutoCompletion(
      childResponses: Set[Future[SubmitResponse]],
      parentId: Id,
      parentCommand: ControlCommand,
      crm: CommandResponseManager
  )(implicit ctx: ActorContext[_]): Completer =
    new Completer(CompleterActor.withAutoCompletion(childResponses, parentId, parentCommand, crm))

}
