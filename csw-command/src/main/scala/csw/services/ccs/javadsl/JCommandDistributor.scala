package csw.services.ccs.javadsl

import java.util
import java.util.concurrent.CompletableFuture

import akka.actor.Scheduler
import akka.stream.Materializer
import akka.util.Timeout
import csw.messages.ccs.commands.{CommandResponse, ControlCommand, JCommandService}
import csw.services.ccs.scaladsl.CommandDistributor

import scala.collection.JavaConverters.{collectionAsScalaIterableConverter, mapAsScalaMapConverter}
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

/**
 * Java API for [[csw.services.ccs.scaladsl.CommandDistributor]]
 * @param componentToCommands a map of Component and the set of commands to be sent to that component
 */
case class JCommandDistributor(componentToCommands: util.Map[JCommandService, util.Set[ControlCommand]]) {

  /**
   * Submit multiple long running commands to components and get an aggregated response as `Accepted` if all the commands
   * were validated successfully, an `Error` otherwise
   * @return an aggregated response as CompletableFuture of CommandResponse
   */
  def aggregatedValidationResponse(
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] = {

    val sComponentToCommands = componentToCommands.asScala.toMap.map {
      case (component, commands) ⇒ component.sComponentRef -> commands.asScala.toSet
    }
    CommandDistributor(sComponentToCommands)
      .aggregatedValidationResponse()(timeout, scheduler, ec, mat)
      .toJava
      .toCompletableFuture
  }

  /**
   * Submit multiple commands to components and subscribe for the final result for long running commands to create
   * an aggregated response as `Completed` if all the commands completed successfully or `Error` if any one of the
   * commands failed.
   * @return an aggregated response as CompletableFuture of CommandResponse
   */
  def aggregatedCompletionResponse(
      timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[CommandResponse] = {

    val sComponentToCommands = componentToCommands.asScala.toMap.map {
      case (component, commands) ⇒ component.sComponentRef -> commands.asScala.toSet
    }

    CommandDistributor(sComponentToCommands)
      .aggregatedCompletionResponse()(timeout, scheduler, ec, mat)
      .toJava
      .toCompletableFuture
  }
}
