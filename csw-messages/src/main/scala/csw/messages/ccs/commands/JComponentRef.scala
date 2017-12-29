package csw.messages.ccs.commands

import java.util.concurrent.CompletableFuture

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.javadsl.Source
import akka.typed.ActorRef
import akka.util.Timeout
import csw.messages.ComponentMessage
import csw.messages.params.models.RunId

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

case class JComponentRef(value: ActorRef[ComponentMessage]) {
  private val componentRef = ComponentRef(value)

  def submit(controlCommand: ControlCommand, timeout: Timeout, scheduler: Scheduler): CompletableFuture[CommandResponse] =
    componentRef.submit(controlCommand)(timeout, scheduler).toJava.toCompletableFuture

  def submitAll(controlCommands: Set[ControlCommand], timeout: Timeout, scheduler: Scheduler): Source[CommandResponse, NotUsed] =
    componentRef.submitAll(controlCommands)(timeout, scheduler).asJava

  def submitAllAndGetResponse(
      controlCommands: Set[ControlCommand],
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] =
    componentRef.submitAllAndGetResponse(controlCommands)(timeout, scheduler, ec, mat).toJava.toCompletableFuture

  def oneway(controlCommand: ControlCommand, timeout: Timeout, scheduler: Scheduler): CompletableFuture[CommandResponse] =
    componentRef.submit(controlCommand)(timeout, scheduler).toJava.toCompletableFuture

  def subscribe(commandRunId: RunId, timeout: Timeout, scheduler: Scheduler): CompletableFuture[CommandResponse] =
    componentRef.subscribe(commandRunId)(timeout, scheduler).toJava.toCompletableFuture

  def submitAndSubscribe(
      controlCommand: ControlCommand,
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): CompletableFuture[CommandResponse] =
    componentRef.submitAndSubscribe(controlCommand)(timeout, scheduler, ec).toJava.toCompletableFuture

  def submitAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher,
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] =
    componentRef.submitAndMatch(controlCommand, stateMatcher)(timeout, scheduler, ec, mat).toJava.toCompletableFuture

  def submitAllAndSubscribe(
      controlCommands: Set[ControlCommand],
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Source[CommandResponse, NotUsed] =
    componentRef.submitAllAndSubscribe(controlCommands)(timeout, scheduler, ec).asJava

  def submitAllAndGetFinalResponse(
      controlCommands: Set[ControlCommand],
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] =
    componentRef.submitAllAndGetFinalResponse(controlCommands)(timeout, scheduler, ec, mat).toJava.toCompletableFuture
}
