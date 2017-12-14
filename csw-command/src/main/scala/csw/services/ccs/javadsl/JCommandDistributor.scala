package csw.services.ccs.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.Scheduler
import akka.stream.Materializer
import akka.typed.ActorRef
import akka.util.Timeout
import csw.messages.ComponentMessage
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.services.ccs.scaladsl.CommandDistributor

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

case class JCommandDistributor(componentToCommands: java.util.Map[ActorRef[ComponentMessage], Set[ControlCommand]]) {

  def execute(
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] = {

    CommandDistributor(componentToCommands.asScala.toMap).execute()(timeout, scheduler, ec, mat).toJava.toCompletableFuture
  }
}
