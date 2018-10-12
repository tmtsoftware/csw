package csw.command.javadsl

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.actor.Scheduler
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import csw.command.models.matchers.StateMatcher
import csw.params.commands.ControlCommand
import csw.location.api.models.AkkaLocation
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.command.scaladsl.{CommandService, CurrentStateSubscription}
import csw.params.commands.CommandResponse.{MatchingResponse, OnewayResponse, QueryResponse, SubmitResponse}

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.compat.java8.FunctionConverters.enrichAsScalaFromConsumer
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

/**
 * Helper class for Java to get the handle of [[csw.command.scaladsl.CommandService]]
 */
class JCommandService(akkaLocation: AkkaLocation, actorSystem: ActorSystem[_]) {

  implicit val ec: ExecutionContext = actorSystem.executionContext
  implicit val mat: Materializer    = ActorMaterializer()(actorSystem.toUntyped)
  implicit val scheduler: Scheduler = actorSystem.scheduler

  private[command] val sCommandService = new CommandService(akkaLocation)(actorSystem)

  /**
   * Submit a command and get a [[csw.params.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse
   * can be obtained by using `subscribe` API.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a CommandResponse as a CompletableFuture
   */
  def submit(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[SubmitResponse] =
    sCommandService.submit(controlCommand)(timeout).toJava.toCompletableFuture

  /**
   * Submit multiple commands and get a Source of [[csw.params.commands.CommandResponse]] for all commands. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse can be obtained by using `subscribe` API.
   *
   * @param controlCommands the set of [[csw.params.commands.ControlCommand]] payloads
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  /*
  def submitAll(controlCommands: java.util.Set[ControlCommand], timeout: Timeout): Source[SubmitResponse, NotUsed] =
    sCommandService.submitAll(controlCommands.asScala.toSet)(timeout).asJava
   */

  /**
   * Submit multiple commands and get one CommandResponse as a Future of [[csw.params.commands.CommandResponse]] for all commands. If all the commands were successful,
   * a CommandResponse as [[csw.params.commands.CommandResponse.Completed]] will be returned. If any one of the command fails, an [[csw.params.commands.CommandResponse.Error]]
   * will be returned.
   *
   * @param controlCommands the set of [[csw.messages.commands.ControlCommand]] payloads
   * @return [[csw.messages.commands.ValidationResponse.Accepted]] or [[csw.messages.commands.CommandResponse.Error]] CommandResponse as a CompletableFuture.
   */
  /*
  def submitAllAndGetResponse(
      controlCommands: java.util.Set[ControlCommand],
      timeout: Timeout
  ): CompletableFuture[SubmitResponse] =
    sCommandService.submitAllAndGetResponse(controlCommands.asScala.toSet)(timeout).toJava.toCompletableFuture
   */

  /**
   * Send a command as a Oneway and get a [[csw.params.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a CommandResponse as a CompletableFuture
   */
  def oneway(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[OnewayResponse] =
    sCommandService.oneway(controlCommand)(timeout).toJava.toCompletableFuture

  /**
   * Subscribe for the result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse]] as a Future.
   *
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a CompletableFuture
   */
  def subscribe(commandRunId: Id, timeout: Timeout): CompletableFuture[SubmitResponse] =
    sCommandService.getFinalResponse(commandRunId)(timeout).toJava.toCompletableFuture

  /**
   * Query for the result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse]] as a Future.
   *
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a CompletableFuture
   */
  def query(commandRunId: Id, timeout: Timeout): CompletableFuture[QueryResponse] =
    sCommandService.query(commandRunId)(timeout).toJava.toCompletableFuture

  /**
   * Submit a command and Subscribe for the result if it was successfully validated as `Accepted` to get a final [[csw.params.commands.CommandResponse]] as a Future.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload.
   * @return a CommandResponse as a CompletableFuture
   */
  def submitAndSubscribe(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[SubmitResponse] =
    sCommandService.submitAndGetFinalResponse(controlCommand)(timeout).toJava.toCompletableFuture

  /**
   * Submit a command and match the published state from the component using a [[csw.command.models.matchers.StateMatcher]]. If the match is successful a `Completed` response is
   * provided as a future. In case of a failure or unmatched state, `Error` CommandResponse is provided as a Future.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload.
   * @param stateMatcher   the StateMatcher implementation for matching received state against a demand state.
   * @return a CommandResponse as a CompletableFuture
   */
  def onewayAndMatch(controlCommand: ControlCommand,
                     stateMatcher: StateMatcher,
                     timeout: Timeout): CompletableFuture[MatchingResponse] =
    sCommandService.onewayAndMatch(controlCommand, stateMatcher)(timeout).toJava.toCompletableFuture

  /**
   * Submit multiple commands and get final CommandResponse for all as a stream of CommandResponse. For long running commands, it will subscribe for the
   * result of those which were successfully validated as `Accepted` and get the final CommandResponse.
   *
   * @param controlCommands the [[csw.params.commands.ControlCommand]] payload.
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  /*
  def submitAllAndSubscribe(controlCommands: java.util.Set[ControlCommand], timeout: Timeout): Source[SubmitResponse, NotUsed] =
    sCommandService.submitAllAndSubscribe(controlCommands.asScala.toSet)(timeout).asJava
   */
  /**
   * Submit multiple commands and get final CommandResponse for all as one CommandResponse. If all the commands were successful, a CommandResponse as
   * [[csw.params.commands.CommandResponse.Completed]] will be returned. If any one of the command fails, an [[csw.params.commands.CommandResponse.Error]]
   * will be returned. For long running commands, it will subscribe for the result of those which were successfully validated as `Accepted` and get the final CommandResponse.
   *
   * @param controlCommands the [[csw.params.commands.ControlCommand]] payload.
   * @return a CommandResponse as a CompletableFuture
   */
  /*
  def submitAllAndGetFinalResponse(
      controlCommands: java.util.Set[ControlCommand],
      timeout: Timeout
  ): CompletableFuture[SubmitResponse] =
    sCommandService.submitAllAndGetFinalResponse(controlCommands.asScala.toSet)(timeout).toJava.toCompletableFuture
   */

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.api.models.AkkaLocation]] of the component
   *
   * @param callback the action to be applied on the CurrentState element received as a result of subscription
   * @return a CurrentStateSubscription to stop the subscription
   */
  def subscribeCurrentState(callback: Consumer[CurrentState]): CurrentStateSubscription =
    sCommandService.subscribeCurrentState(callback.asScala)

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.api.models.AkkaLocation]] of the component
   *
   * @param names    subscribe to only those states which have any of the the provided value for name
   * @param callback the action to be applied on the CurrentState element received as a result of subscription
   * @return a CurrentStateSubscription to stop the subscription
   */
  def subscribeOnlyCurrentState(
      names: java.util.Set[StateName],
      callback: Consumer[CurrentState]
  ): CurrentStateSubscription =
    sCommandService.subscribeOnlyCurrentState(names.asScala.toSet, callback.asScala)

}
