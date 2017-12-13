package csw.services.ccs.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.Scheduler
import akka.stream.Materializer
import akka.typed.ActorRef
import csw.messages.ComponentMessage
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}

import scala.compat.java8.FutureConverters._
import akka.util.Timeout
import csw.messages.params.models.RunId
import csw.services.ccs.common.ActorRefExts.RichComponentActor

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.concurrent.ExecutionContext

object CommandExecutionService {
  def submit(
      actorRef: ActorRef[ComponentMessage],
      controlCommand: ControlCommand,
      timeout: Timeout,
      scheduler: Scheduler
  ): CompletableFuture[CommandResponse] =
    actorRef.submit(controlCommand)(timeout, scheduler).toJava.toCompletableFuture

  def oneway(
      actorRef: ActorRef[ComponentMessage],
      controlCommand: ControlCommand,
      timeout: Timeout,
      scheduler: Scheduler
  ): CompletableFuture[CommandResponse] = actorRef.submit(controlCommand)(timeout, scheduler).toJava.toCompletableFuture

  def getCommandResponse(
      actorRef: ActorRef[ComponentMessage],
      commandRunId: RunId,
      timeout: Timeout,
      scheduler: Scheduler
  ): CompletableFuture[CommandResponse] = actorRef.getCommandResponse(commandRunId)(timeout, scheduler).toJava.toCompletableFuture

  def submitAndGetCommandResponse(
      actorRef: ActorRef[ComponentMessage],
      controlCommand: ControlCommand,
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): CompletableFuture[CommandResponse] =
    actorRef.submitAndGetCommandResponse(controlCommand)(timeout, scheduler, ec).toJava.toCompletableFuture

  def submitManyAndGetCommandResponse(
      actorRef: ActorRef[ComponentMessage],
      controlCommands: java.util.Set[ControlCommand],
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] =
    actorRef
      .submitManyAndGetCommandResponse(controlCommands.asScala.toSet)(timeout, scheduler, ec, mat)
      .toJava
      .toCompletableFuture
}
