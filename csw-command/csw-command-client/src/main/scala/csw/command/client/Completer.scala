package csw.command.client

import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.command.client.CompleterActor.CommandCompleterMessage.{Update, WaitComplete}
import csw.command.client.CompleterActor.{CommandCompleterMessage, OverallResponse}
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse.{QueryResponse, SubmitResponse}
import csw.params.commands.ControlCommand
import csw.params.core.models.Id

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

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
  def waitComplete()(implicit timeout: Timeout = Timeout(5.seconds)): Future[OverallResponse] = {
    implicit val scheduler: Scheduler = ctx.system.scheduler
    ref ? WaitComplete
  }
}

object Completer {
  def make(responses: Set[Future[SubmitResponse]], loggerFactory: LoggerFactory)(implicit ctx: ActorContext[_]) =
    new Completer(CompleterActor.make(responses, loggerFactory))

  def withAutoCompletion(
      parentId: Id,
      parentCommand: ControlCommand,
      childResponses: Set[Future[SubmitResponse]],
      loggerFactory: LoggerFactory,
      crm: CommandResponseManager
  )(implicit ctx: ActorContext[_]): Completer =
    new Completer(CompleterActor.withAutoCompletion(parentId, parentCommand, childResponses, loggerFactory, crm))

}
