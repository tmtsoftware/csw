package csw.services.ccs.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.Scheduler
import akka.typed.ActorRef
import csw.messages.SupervisorExternalMessage
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import scala.compat.java8.FutureConverters._

import akka.util.Timeout
import csw.messages.params.models.RunId
import csw.services.ccs.common.ActorRefExts.RichComponentActor

object CommandExecutionService {
  def submit(
      actorRef: ActorRef[SupervisorExternalMessage],
      controlCommand: ControlCommand,
      timeout: Timeout,
      scheduler: Scheduler
  ): CompletableFuture[CommandResponse] =
    actorRef.submit(controlCommand)(timeout, scheduler).toJava.toCompletableFuture

  def oneway(
      actorRef: ActorRef[SupervisorExternalMessage],
      controlCommand: ControlCommand,
      timeout: Timeout,
      scheduler: Scheduler
  ): CompletableFuture[CommandResponse] = actorRef.submit(controlCommand)(timeout, scheduler).toJava.toCompletableFuture

  def getCommandResponse(
      actorRef: ActorRef[SupervisorExternalMessage],
      commandRunId: RunId,
      timeout: Timeout,
      scheduler: Scheduler
  ): CompletableFuture[CommandResponse] = actorRef.getCommandResponse(commandRunId)(timeout, scheduler).toJava.toCompletableFuture
}
