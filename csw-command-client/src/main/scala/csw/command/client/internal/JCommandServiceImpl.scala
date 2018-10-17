package csw.command.client.internal

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.actor.Scheduler
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import csw.command.api.javadsl.ICommandService
import csw.command.api.{CurrentStateSubscription, StateMatcher}
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse.{MatchingResponse, OnewayResponse, QueryResponse, SubmitResponse}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.collection.JavaConverters.{iterableAsScalaIterableConverter, seqAsJavaListConverter}
import scala.compat.java8.FunctionConverters.enrichAsScalaFromConsumer
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

private[csw] class JCommandServiceImpl(akkaLocation: AkkaLocation, actorSystem: ActorSystem[_]) extends ICommandService {

  implicit val ec: ExecutionContext = actorSystem.executionContext
  implicit val mat: Materializer    = ActorMaterializer()(actorSystem.toUntyped)
  implicit val scheduler: Scheduler = actorSystem.scheduler

  private[command] val sCommandService = new CommandServiceImpl(akkaLocation)(actorSystem)

  def completeAll(
      controlCommand: java.util.List[ControlCommand],
      timeout: Timeout
  ): CompletableFuture[java.util.List[SubmitResponse]] =
    sCommandService.completeAll(controlCommand.asScala.toList)(timeout).map(_.asJava).toJava.toCompletableFuture

  def submit(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[SubmitResponse] =
    sCommandService.submit(controlCommand)(timeout).toJava.toCompletableFuture

  def oneway(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[OnewayResponse] =
    sCommandService.oneway(controlCommand)(timeout).toJava.toCompletableFuture

  def queryFinal(commandRunId: Id, timeout: Timeout): CompletableFuture[SubmitResponse] =
    sCommandService.queryFinal(commandRunId)(timeout).toJava.toCompletableFuture

  def query(commandRunId: Id, timeout: Timeout): CompletableFuture[QueryResponse] =
    sCommandService.query(commandRunId)(timeout).toJava.toCompletableFuture

  def complete(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[SubmitResponse] =
    sCommandService.complete(controlCommand)(timeout).toJava.toCompletableFuture

  def onewayAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher,
      timeout: Timeout
  ): CompletableFuture[MatchingResponse] =
    sCommandService.onewayAndMatch(controlCommand, stateMatcher)(timeout).toJava.toCompletableFuture

  def subscribeCurrentState(callback: Consumer[CurrentState]): CurrentStateSubscription =
    sCommandService.subscribeCurrentState(callback.asScala)

  def subscribeCurrentState(
      names: java.util.Set[StateName],
      callback: Consumer[CurrentState]
  ): CurrentStateSubscription = sCommandService.subscribeCurrentState(names.asScala.toSet, callback.asScala)

  override def validate(
      controlCommand: ControlCommand,
      timeout: Timeout
  ): CompletableFuture[CommandResponse.ValidateResponse] =
    sCommandService.validate(controlCommand)(timeout).toJava.toCompletableFuture
}
