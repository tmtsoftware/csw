package csw.command.scaladsl

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.models.CommandResponseAggregator
import csw.messages.commands.CommandResponse.Completed
import csw.messages.commands.{CommandIssue, CommandResponse, ControlCommand}
import csw.messages.params.models.Id

import scala.concurrent.{ExecutionContext, Future}

/**
 * The ConfigDistributor enables distributing multiple commands to multiple components and get one aggregated command
 * response as a final response
 *
 * @param componentToCommands a map of Component and the set of commands to be sent to that component
 */
case class CommandDistributor(componentToCommands: Map[CommandService, Set[ControlCommand]]) {

  private val breadth = 10

  /**
   * Submit multiple long running commands to components and get an aggregated response as `Accepted` if all the commands
   * were validated successfully, an `Error` otherwise
   *
   * @return an aggregated response as Future value of CommandResponse
   */
  def aggregatedValidationResponse()(
      implicit timeout: Timeout,
      ec: ExecutionContext,
      mat: Materializer
  ): Future[CommandResponse] = {

    val commandResponsesF: Source[CommandResponse, NotUsed] = Source(componentToCommands).flatMapMerge(
      breadth,
      { case (component, commands) ⇒ component.submitAll(commands) }
    )
    CommandResponseAggregator.aggregateResponse(commandResponsesF).map {
      case _: Completed  ⇒ CommandResponse.Accepted(Id())
      case otherResponse ⇒ CommandResponse.Invalid(Id(), CommandIssue.OtherIssue("One or more commands were Invalid"))
    }
  }

  /**
   * Submit multiple commands to components and subscribe for the final result for long running commands to create
   * an aggregated response as `Completed` if all the commands completed successfully or `Error` if any one of the
   * commands failed.
   *
   * @return an aggregated response as Future value of CommandResponse
   */
  def aggregatedCompletionResponse()(
      implicit timeout: Timeout,
      ec: ExecutionContext,
      mat: Materializer
  ): Future[CommandResponse] = {

    val commandResponsesF: Source[CommandResponse, NotUsed] = Source(componentToCommands).flatMapMerge(
      breadth,
      { case (component, commands) ⇒ component.submitAllAndSubscribe(commands) }
    )
    CommandResponseAggregator.aggregateResponse(commandResponsesF)
  }
}
