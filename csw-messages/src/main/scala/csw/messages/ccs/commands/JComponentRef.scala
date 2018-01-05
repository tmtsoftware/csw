package csw.messages.ccs.commands

import java.util.concurrent.CompletableFuture

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.javadsl.Source
import akka.typed.ActorRef
import akka.util.Timeout
import csw.messages.ComponentMessage
import csw.messages.ccs.commands.matchers.StateMatcher
import csw.messages.params.models.RunId

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

/**
 * Java API for [[csw.messages.ccs.commands.ComponentRef]]
 */
case class JComponentRef(value: ActorRef[ComponentMessage]) {
  private val componentRef = ComponentRef(value)

  /**
   * Submit a command and get a [[csw.messages.ccs.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse
   * can be obtained by using `subscribe` API.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload
   * @return a CommandResponse as a CompletableFuture
   */
  def submit(controlCommand: ControlCommand, timeout: Timeout, scheduler: Scheduler): CompletableFuture[CommandResponse] =
    componentRef.submit(controlCommand)(timeout, scheduler).toJava.toCompletableFuture

  /**
   * Submit multiple commands and get a Source of [[csw.messages.ccs.commands.CommandResponse]] for all commands. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse can be obtained by using `subscribe` API.
   * @param controlCommands the set of [[csw.messages.ccs.commands.ControlCommand]] payloads
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  def submitAll(controlCommands: Set[ControlCommand], timeout: Timeout, scheduler: Scheduler): Source[CommandResponse, NotUsed] =
    componentRef.submitAll(controlCommands)(timeout, scheduler).asJava

  /**
   * Submit multiple commands and get one CommandResponse as a Future of [[csw.messages.ccs.commands.CommandResponse]] for all commands. If all the commands were successful,
   * a CommandResponse as [[csw.messages.ccs.commands.CommandResponse.Completed]] will be returned. If any one of the command fails, an [[csw.messages.ccs.commands.CommandResponse.Error]]
   * will be returned.
   * @param controlCommands the set of [[csw.messages.ccs.commands.ControlCommand]] payloads
   * @return [[csw.messages.ccs.commands.CommandResponse.Accepted]] or [[csw.messages.ccs.commands.CommandResponse.Error]] CommandResponse as a CompletableFuture.
   */
  def submitAllAndGetResponse(
      controlCommands: Set[ControlCommand],
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] =
    componentRef.submitAllAndGetResponse(controlCommands)(timeout, scheduler, ec, mat).toJava.toCompletableFuture

  /**
   * Send a command as a Oneway and get a [[csw.messages.ccs.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload
   * @return a CommandResponse as a CompletableFuture
   */
  def oneway(controlCommand: ControlCommand, timeout: Timeout, scheduler: Scheduler): CompletableFuture[CommandResponse] =
    componentRef.submit(controlCommand)(timeout, scheduler).toJava.toCompletableFuture

  /**
   * Subscribe for the result of a long running command which was sent as Submit to get a [[csw.messages.ccs.commands.CommandResponse]] as a Future.
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a CompletableFuture
   */
  def subscribe(commandRunId: RunId, timeout: Timeout, scheduler: Scheduler): CompletableFuture[CommandResponse] =
    componentRef.subscribe(commandRunId)(timeout, scheduler).toJava.toCompletableFuture

  /**
   * Submit a command and Subscribe for the result if it was successfully validated as `Accepted` to get a final [[csw.messages.ccs.commands.CommandResponse]] as a Future.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @return a CommandResponse as a CompletableFuture
   */
  def submitAndSubscribe(
      controlCommand: ControlCommand,
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): CompletableFuture[CommandResponse] =
    componentRef.submitAndSubscribe(controlCommand)(timeout, scheduler, ec).toJava.toCompletableFuture

  /**
   * Submit a command and match the published state from the component using a [[StateMatcher]]. If the match is successful a `Completed` response is
   * provided as a future. In case of a failure or unmatched state, `Error` CommandResponse is provided as a Future.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @param stateMatcher the StateMatcher implementation for matching received state against a demand state.
   * @return a CommandResponse as a CompletableFuture
   */
  def submitAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher,
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] =
    componentRef.submitAndMatch(controlCommand, stateMatcher)(timeout, scheduler, ec, mat).toJava.toCompletableFuture

  /**
   * Submit multiple commands and get final CommandResponse for all as a stream of CommandResponse. For long running commands, it will subscribe for the
   * result of those which were successfully validated as `Accepted` and get the final CommandResponse.
   * @param controlCommands the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  def submitAllAndSubscribe(
      controlCommands: Set[ControlCommand],
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Source[CommandResponse, NotUsed] =
    componentRef.submitAllAndSubscribe(controlCommands)(timeout, scheduler, ec).asJava

  /**
   * Submit multiple commands and get final CommandResponse for all as one CommandResponse. If all the commands were successful, a CommandResponse as
   * [[csw.messages.ccs.commands.CommandResponse.Completed]] will be returned. If any one of the command fails, an [[csw.messages.ccs.commands.CommandResponse.Error]]
   * will be returned. For long running commands, it will subscribe for the result of those which were successfully validated as `Accepted` and get the final CommandResponse.
   * @param controlCommands the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @return a CommandResponse as a CompletableFuture
   */
  def submitAllAndGetFinalResponse(
      controlCommands: Set[ControlCommand],
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] =
    componentRef.submitAllAndGetFinalResponse(controlCommands)(timeout, scheduler, ec, mat).toJava.toCompletableFuture
}
