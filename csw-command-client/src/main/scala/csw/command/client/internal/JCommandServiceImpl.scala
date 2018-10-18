package csw.command.client.internal

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.util.Timeout
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.command.api.{CurrentStateSubscription, StateMatcher}
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.collection.JavaConverters.{iterableAsScalaIterableConverter, seqAsJavaListConverter}
import scala.compat.java8.FunctionConverters.enrichAsScalaFromConsumer
import scala.compat.java8.FutureConverters.FutureOps

private[command] class JCommandServiceImpl(commandService: CommandService) extends ICommandService {

  override def validate(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[ValidateResponse] =
    commandService.validate(controlCommand)(timeout).toJava.toCompletableFuture

  override def submit(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[SubmitResponse] =
    commandService.submit(controlCommand)(timeout).toJava.toCompletableFuture

  override def submitAll(
      controlCommand: java.util.List[ControlCommand],
      timeout: Timeout
  ): CompletableFuture[java.util.List[SubmitResponse]] =
    commandService
      .submitAll(controlCommand.asScala.toList)(timeout)
      .toJava
      .toCompletableFuture
      .thenApply(_.asJava)

  override def oneway(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[OnewayResponse] =
    commandService.oneway(controlCommand)(timeout).toJava.toCompletableFuture

  override def onewayAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher,
      timeout: Timeout
  ): CompletableFuture[MatchingResponse] =
    commandService.onewayAndMatch(controlCommand, stateMatcher)(timeout).toJava.toCompletableFuture

  override def query(commandRunId: Id, timeout: Timeout): CompletableFuture[QueryResponse] =
    commandService.query(commandRunId)(timeout).toJava.toCompletableFuture

  override def subscribeCurrentState(callback: Consumer[CurrentState]): CurrentStateSubscription =
    commandService.subscribeCurrentState(callback.asScala)

  override def subscribeCurrentState(
      names: java.util.Set[StateName],
      callback: Consumer[CurrentState]
  ): CurrentStateSubscription = commandService.subscribeCurrentState(names.asScala.toSet, callback.asScala)
}
