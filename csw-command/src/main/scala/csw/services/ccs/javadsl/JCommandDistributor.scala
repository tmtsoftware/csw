package csw.services.ccs.javadsl

import java.util
import java.util.concurrent.CompletableFuture

import akka.actor.Scheduler
import akka.stream.Materializer
import akka.util.Timeout
import csw.messages.ccs.commands.{CommandResponse, ComponentRef, ControlCommand, JComponentRef}
import csw.services.ccs.scaladsl.CommandDistributor

import scala.collection.JavaConverters.{collectionAsScalaIterableConverter, mapAsScalaMapConverter}
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

case class JCommandDistributor(componentToCommands: util.Map[JComponentRef, util.Set[ControlCommand]]) {

  def aggregatedValidationResponse(
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] = {

    val sComponentToCommands = componentToCommands.asScala.toMap.map {
      case (component, commands) ⇒ ComponentRef(component.value) -> commands.asScala.toSet
    }
    CommandDistributor(sComponentToCommands)
      .aggregatedValidationResponse()(timeout, scheduler, ec, mat)
      .toJava
      .toCompletableFuture
  }

  def aggregatedCompletionResponse(
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] = {

    val sComponentToCommands = componentToCommands.asScala.toMap.map {
      case (component, commands) ⇒ ComponentRef(component.value) -> commands.asScala.toSet
    }

    CommandDistributor(sComponentToCommands)
      .aggregatedCompletionResponse()(timeout, scheduler, ec, mat)
      .toJava
      .toCompletableFuture
  }
}
