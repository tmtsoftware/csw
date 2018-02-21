package csw.services.ccs.scaladsl

import java.util.concurrent.CompletableFuture

import akka.actor.Scheduler
import akka.stream.{ActorMaterializer, Materializer}
import akka.typed.scaladsl.AskPattern._
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.messages.CommandResponseManagerMessage
import csw.messages.CommandResponseManagerMessage._
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.Id
import csw.services.ccs.internal.CommandResponseSubscription

import scala.compat.java8.FutureConverters._
import scala.concurrent.Future

class CommandResponseManager(val commandResponseManagerActor: ActorRef[CommandResponseManagerMessage])(
    implicit val actorSystem: ActorSystem[_]
) {

  private implicit val mat: Materializer    = ActorMaterializer()(actorSystem.toUntyped)
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  def addOrUpdateCommand(commandId: Id, cmdStatus: CommandResponse): Unit =
    commandResponseManagerActor ! AddOrUpdateCommand(commandId, cmdStatus)

  def addSubCommand(parentRunId: Id, childRunId: Id): Unit =
    commandResponseManagerActor ! AddSubCommand(parentRunId, childRunId)

  def updateSubCommand(subCommandId: Id, cmdStatus: CommandResponse): Unit =
    commandResponseManagerActor ! UpdateSubCommand(subCommandId, cmdStatus)

  def query(runId: Id)(implicit timeout: Timeout): Future[CommandResponse] =
    commandResponseManagerActor ? (Query(runId, _))

  def jQuery(runId: Id, timeout: Timeout): CompletableFuture[CommandResponse] = {
    query(runId)(timeout).toJava.toCompletableFuture
  }

  def subscribe(runId: Id, callback: CommandResponse ⇒ Unit): CommandResponseSubscription = {
    new CommandResponseSubscription(runId, commandResponseManagerActor, callback)
  }

  def jSubscribe(runId: Id, callback: CommandResponse ⇒ Unit): CommandResponseSubscription = {
    new CommandResponseSubscription(runId, commandResponseManagerActor, callback)
  }

}
