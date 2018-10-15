package csw.command.scaladsl

import csw.params.commands.ControlCommand

/**
 * The ConfigDistributor enables distributing multiple commands to multiple components and get one aggregated command
 * response as a final response
 *
 * @param componentToCommands a map of Component and the set of commands to be sent to that component
 */
case class CommandDistributor(componentToCommands: Map[CommandService, ControlCommand]) {

  private val breadth = 10

  /**
   * Submit multiple long running commands to components and get an aggregated response as `Accepted` if all the commands
   * were validated successfully, an `Error` otherwise
   *
   * @return an aggregated response as Future value of CommandResponse
   */
  /*
  def aggregatedValidationResponse()(
      implicit timeout: Timeout,
      ec: ExecutionContext,
      mat: Materializer
  ): Future[SubmitResponse] = {

    val commandResponsesF: Source[SubmitResponse, NotUsed] = Source(componentToCommands).flatMap {cs =>
      cs._1.submit(cs._2)
    }
    /*
    val commandResponsesF: Source[SubmitResponse, NotUsed] = Source(componentToCommands).flatMapMerge(
      breadth,
      { case (component, commands) ⇒ component.submitAll(commands) }
    )
 */
    CommandResponse.aggregateResponse(commandResponsesF).map {
      case _: Started    ⇒ CommandResponse.Started(Id())
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
  ): Future[SubmitResponse] = {

    val commandResponsesF: Source[SubmitResponse, NotUsed] = Source(componentToCommands).flatMapMerge(
      breadth,
      { case (component, commands) ⇒ component.submitAllAndSubscribe(commands) }
    )
    CommandResponseAggregator.aggregateResponse(commandResponsesF)
  }
 */
}
