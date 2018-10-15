package csw.command.client

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.{ActorSystem, Scheduler}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import csw.command.client.internal.messages.CommandResponseManagerMessage
import csw.command.client.internal.messages.CommandResponseManagerMessage.{
  AddOrUpdateCommand,
  AddSubCommand,
  Query,
  UpdateSubCommand
}
import csw.params.commands.CommandResponse.{QueryResponse, SubmitResponse}
import csw.params.core.models.Id

import scala.compat.java8.FunctionConverters.enrichAsScalaFromConsumer
import scala.compat.java8.FutureConverters._
import scala.concurrent.Future

/**
 * Wrapper API for interacting with Command Response Manager of a component
 *
 * @param commandResponseManagerActor the wrapped actor
 * @param actorSystem actor system for managing stream resources inside
 */
class CommandResponseManager private[command] (val commandResponseManagerActor: ActorRef[CommandResponseManagerMessage])(
    implicit val actorSystem: ActorSystem
) {

  private implicit val mat: Materializer    = ActorMaterializer()(actorSystem)
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  /**
   * Add a new command or update an existing command with the provided status
   *
   * @param runId command identifier
   * @param cmdStatus status of command as [[csw.params.commands.CommandResponse]]
   */
  def addOrUpdateCommand(runId: Id, cmdStatus: SubmitResponse): Unit =
    commandResponseManagerActor ! AddOrUpdateCommand(runId, cmdStatus)

  /**
   * Add a new sub command against another command
   *
   * @param parentRunId command identifier of original command
   * @param childRunId command identifier of sub command
   */
  def addSubCommand(parentRunId: Id, childRunId: Id): Unit =
    commandResponseManagerActor ! AddSubCommand(parentRunId, childRunId)

  /**
   * Update the status of a sub-command which will infer the status of the parent command
   *
   * @param subCommandId command identifier of sub command
   * @param cmdStatus status of command as [[csw.params.commands.CommandResponse]]
   */
  def updateSubCommand(subCommandId: Id, cmdStatus: SubmitResponse): Unit =
    commandResponseManagerActor ! UpdateSubCommand(subCommandId, cmdStatus)

  /**
   * Query the current status of a command
   *
   * @param runId command identifier of command
   * @param timeout timeout duration until which this operation is expected to wait for providing a value
   * @return a future of CommandResponse
   */
  def query(runId: Id)(implicit timeout: Timeout): Future[QueryResponse] =
    commandResponseManagerActor ? (Query(runId, _))

  /**
   * A helper method for Java to query the current status of a command
   *
   * @param runId command identifier of command
   * @param timeout timeout duration until which this operation is expected to wait for providing a value
   * @return a future of CommandResponse
   */
  def jQuery(runId: Id, timeout: Timeout): CompletableFuture[QueryResponse] =
    query(runId)(timeout).toJava.toCompletableFuture

  /**
   * Subscribe to the status of a command to receive the update in status
   *
   * @param runId command identifier of command
   * @param callback callback  to take action on the command response received
   * @return a [[CommandResponseSubscription]] to unsubscribe the subscription later
   */
  def subscribe(runId: Id, callback: SubmitResponse ⇒ Unit): CommandResponseSubscription =
    new CommandResponseSubscription(runId, commandResponseManagerActor, callback)

  /**
   * A helper method for Java to subscribe to the status of a command to receive the update in status
   * @param runId command identifier of command
   * @param consumer consumer function to take action on the command response received
   * @return a CommandResponseSubscription that can be used to unsubscribe
   */
  def jSubscribe(runId: Id, consumer: Consumer[SubmitResponse]): CommandResponseSubscription =
    new CommandResponseSubscription(runId, commandResponseManagerActor, consumer.asScala)

}
