package csw.services.ccs.javadsl

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.javadsl.Source
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.ActorSystem
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.util.Timeout
import csw.messages.ccs.commands.matchers.StateMatcher
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.location.AkkaLocation
import csw.messages.params.models.Id
import csw.messages.params.states.CurrentState
import csw.services.ccs.internal.CurrentStateSubscription
import csw.services.ccs.scaladsl.CommandService

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.compat.java8.FunctionConverters.enrichAsScalaFromConsumer
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

/**
 * Java API for [[csw.services.ccs.scaladsl.CommandService]]
 */
class JCommandService(akkaLocation: AkkaLocation, actorSystem: ActorSystem[_]) {

  implicit val ec: ExecutionContext = actorSystem.executionContext
  implicit val mat: Materializer    = ActorMaterializer()(actorSystem.toUntyped)
  implicit val scheduler: Scheduler = actorSystem.scheduler

  private[ccs] val sCommandService: CommandService = new CommandService(akkaLocation)(actorSystem)

  /**
   * Submit a command and get a [[csw.messages.ccs.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse
   * can be obtained by using `subscribe` API.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload
   * @return a CommandResponse as a CompletableFuture
   */
  def submit(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[CommandResponse] =
    sCommandService.submit(controlCommand)(timeout).toJava.toCompletableFuture

  /**
   * Submit multiple commands and get a Source of [[csw.messages.ccs.commands.CommandResponse]] for all commands. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse can be obtained by using `subscribe` API.
   * @param controlCommands the set of [[csw.messages.ccs.commands.ControlCommand]] payloads
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  def submitAll(controlCommands: java.util.Set[ControlCommand], timeout: Timeout): Source[CommandResponse, NotUsed] =
    sCommandService.submitAll(controlCommands.asScala.toSet)(timeout).asJava

  /**
   * Submit multiple commands and get one CommandResponse as a Future of [[csw.messages.ccs.commands.CommandResponse]] for all commands. If all the commands were successful,
   * a CommandResponse as [[csw.messages.ccs.commands.CommandResponse.Completed]] will be returned. If any one of the command fails, an [[csw.messages.ccs.commands.CommandResponse.Error]]
   * will be returned.
   * @param controlCommands the set of [[csw.messages.ccs.commands.ControlCommand]] payloads
   * @return [[csw.messages.ccs.commands.CommandResponse.Accepted]] or [[csw.messages.ccs.commands.CommandResponse.Error]] CommandResponse as a CompletableFuture.
   */
  def submitAllAndGetResponse(
      controlCommands: java.util.Set[ControlCommand],
      timeout: Timeout
  ): CompletableFuture[CommandResponse] =
    sCommandService.submitAllAndGetResponse(controlCommands.asScala.toSet)(timeout).toJava.toCompletableFuture

  /**
   * Send a command as a Oneway and get a [[csw.messages.ccs.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload
   * @return a CommandResponse as a CompletableFuture
   */
  def oneway(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[CommandResponse] =
    sCommandService.oneway(controlCommand)(timeout).toJava.toCompletableFuture

  /**
   * Subscribe for the result of a long running command which was sent as Submit to get a [[csw.messages.ccs.commands.CommandResponse]] as a Future.
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a CompletableFuture
   */
  def subscribe(commandRunId: Id, timeout: Timeout): CompletableFuture[CommandResponse] =
    sCommandService.subscribe(commandRunId)(timeout).toJava.toCompletableFuture

  /**
   * Query for the result of a long running command which was sent as Submit to get a [[csw.messages.ccs.commands.CommandResponse]] as a Future.
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a CompletableFuture
   */
  def query(commandRunId: Id, timeout: Timeout): CompletableFuture[CommandResponse] =
    sCommandService.query(commandRunId)(timeout).toJava.toCompletableFuture

  /**
   * Submit a command and Subscribe for the result if it was successfully validated as `Accepted` to get a final [[csw.messages.ccs.commands.CommandResponse]] as a Future.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @return a CommandResponse as a CompletableFuture
   */
  def submitAndSubscribe(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[CommandResponse] =
    sCommandService.submitAndSubscribe(controlCommand)(timeout).toJava.toCompletableFuture

  /**
   * Submit a command and match the published state from the component using a [[csw.messages.ccs.commands.matchers.StateMatcher]]. If the match is successful a `Completed` response is
   * provided as a future. In case of a failure or unmatched state, `Error` CommandResponse is provided as a Future.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @param stateMatcher the StateMatcher implementation for matching received state against a demand state.
   * @return a CommandResponse as a CompletableFuture
   */
  def onewayAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher,
      timeout: Timeout
  ): CompletableFuture[CommandResponse] =
    sCommandService.onewayAndMatch(controlCommand, stateMatcher)(timeout).toJava.toCompletableFuture

  /**
   * Submit multiple commands and get final CommandResponse for all as a stream of CommandResponse. For long running commands, it will subscribe for the
   * result of those which were successfully validated as `Accepted` and get the final CommandResponse.
   * @param controlCommands the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  def submitAllAndSubscribe(controlCommands: java.util.Set[ControlCommand], timeout: Timeout): Source[CommandResponse, NotUsed] =
    sCommandService.submitAllAndSubscribe(controlCommands.asScala.toSet)(timeout).asJava

  /**
   * Submit multiple commands and get final CommandResponse for all as one CommandResponse. If all the commands were successful, a CommandResponse as
   * [[csw.messages.ccs.commands.CommandResponse.Completed]] will be returned. If any one of the command fails, an [[csw.messages.ccs.commands.CommandResponse.Error]]
   * will be returned. For long running commands, it will subscribe for the result of those which were successfully validated as `Accepted` and get the final CommandResponse.
   * @param controlCommands the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @return a CommandResponse as a CompletableFuture
   */
  def submitAllAndGetFinalResponse(
      controlCommands: java.util.Set[ControlCommand],
      timeout: Timeout
  ): CompletableFuture[CommandResponse] =
    sCommandService.submitAllAndGetFinalResponse(controlCommands.asScala.toSet)(timeout).toJava.toCompletableFuture

  //TODO: add doc for significance
  def subscribeCurrentState(callback: Consumer[CurrentState]): CurrentStateSubscription =
    sCommandService.subscribeCurrentState(callback.asScala)

}
