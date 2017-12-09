package csw.services.ccs.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.Scheduler
import akka.util.Timeout
import csw.messages.ActorTypes.ComponentRef
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.params.models.RunId
import csw.services.ccs.common.ActorRefExts.RichComponentActor

import scala.compat.java8.FutureConverters._

object CommandExecutionService {
  def submit(
      actorRef: ComponentRef,
      controlCommand: ControlCommand,
      timeout: Timeout,
      scheduler: Scheduler
  ): CompletableFuture[CommandResponse] = actorRef.submit(controlCommand)(timeout, scheduler).toJava.toCompletableFuture

  def oneway(
      actorRef: ComponentRef,
      controlCommand: ControlCommand,
      timeout: Timeout,
      scheduler: Scheduler
  ): CompletableFuture[CommandResponse] = actorRef.submit(controlCommand)(timeout, scheduler).toJava.toCompletableFuture

  def getCommandResponse(
      actorRef: ComponentRef,
      commandRunId: RunId,
      timeout: Timeout,
      scheduler: Scheduler
  ): CompletableFuture[CommandResponse] = actorRef.getCommandResponse(commandRunId)(timeout, scheduler).toJava.toCompletableFuture
}
