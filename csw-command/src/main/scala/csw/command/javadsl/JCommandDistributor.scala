package csw.command.javadsl

import java.util
import java.util.concurrent.CompletableFuture

import akka.stream.Materializer
import akka.util.Timeout
import csw.messages.commands.{CommandResponseBase, ControlCommand, ValidationResponse}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.command.scaladsl.CommandDistributor
import csw.messages.commands.{CommandResponse, ControlCommand, ValidationResponse}
import csw.services.command.scaladsl.CommandDistributor

import scala.collection.JavaConverters.{collectionAsScalaIterableConverter, mapAsScalaMapConverter}
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

/**
 * Helper class for Java to get the handle of [[csw.command.scaladsl.CommandDistributor]]
 *
 * @param componentToCommands a map of Component and the set of commands to be sent to that component
 */
case class JCommandDistributor(componentToCommands: util.Map[JCommandService, util.Set[ControlCommand]]) {

  /**
   * Submit multiple long running commands to components and get an aggregated response as `Accepted` if all the commands
   * were validated successfully, an `Error` otherwise
   *
   * @return an aggregated response as CompletableFuture of CommandResponse
   */
  /*
  def aggregatedValidationResponse(
      timeout: Timeout,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[SubmitResponse] = {

    val sComponentToCommands = componentToCommands.asScala.toMap.map {
      case (jCommandService, commands) ⇒ jCommandService.sCommandService -> commands.asScala.toSet
    }
    CommandDistributor(sComponentToCommands)
      .aggregatedValidationResponse()(timeout, ec, mat)
      .toJava
      .toCompletableFuture
  }

  /**
 * Submit multiple commands to components and subscribe for the final result for long running commands to create
 * an aggregated response as `Completed` if all the commands completed successfully or `Error` if any one of the
 * commands failed.
 *
 * @return an aggregated response as CompletableFuture of CommandResponse
 */
  def aggregatedCompletionResponse(
      timeout: Timeout,
      ec: ExecutionContext,
      mat: Materializer
  ): CompletableFuture[SubmitResponse] = {

    val sComponentToCommands = componentToCommands.asScala.toMap.map {
      case (jCommandService, commands) ⇒ jCommandService.sCommandService -> commands.asScala.toSet
    }

    CommandDistributor(sComponentToCommands)
      .aggregatedCompletionResponse()(timeout, ec, mat)
      .toJava
      .toCompletableFuture
  }
 */
}
