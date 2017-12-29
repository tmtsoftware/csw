package csw.messages.ccs.commands

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed, Error}
import csw.messages.ccs.commands.matchers.Matcher
import csw.messages.ccs.commands.matchers.MatcherResponses.{MatchCompleted, MatchFailed}
import csw.messages.params.models.RunId
import csw.messages.{CommandResponseManagerMessage, ComponentMessage}

import scala.concurrent.{ExecutionContext, Future}

/**
 * A wrapper of an ActorRef of a csw component. This model provides method based APIs for command interactions with a component.
 * @param value ActorRef[ComponentMessage] of underlying component
 */
case class ComponentRef(value: ActorRef[ComponentMessage]) {

  private val parallelism = 10

  /**
   * Submit a command and get a [[csw.messages.ccs.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse
   * can be obtained by using `subscribe` API.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload
   * @return a CommandResponse as a Future value.
   */
  def submit(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
    value ? (Submit(controlCommand, _))

  /**
   * Submit multiple commands and get a Source of [[csw.messages.ccs.commands.CommandResponse]] for all commands. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse can be obtained by using `subscribe` API.
   * @param controlCommands the set of [[csw.messages.ccs.commands.ControlCommand]] payloads
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  def submitAll(
      controlCommands: Set[ControlCommand]
  )(implicit timeout: Timeout, scheduler: Scheduler): Source[CommandResponse, NotUsed] = {
    Source(controlCommands).mapAsyncUnordered(parallelism)(submit)
  }

  /**
   * Submit multiple commands and get one CommandResponse as a Future of [[csw.messages.ccs.commands.CommandResponse]] for all commands. If all the commands were successful,
   * a CommandResponse as [[csw.messages.ccs.commands.CommandResponse.Completed]] will be returned. If any one of the command fails, an [[csw.messages.ccs.commands.CommandResponse.Error]]
   * will be returned.
   * @param controlCommands the set of [[csw.messages.ccs.commands.ControlCommand]] payloads
   * @return [[csw.messages.ccs.commands.CommandResponse.Accepted]] or [[csw.messages.ccs.commands.CommandResponse.Error]] CommandResponse as a Future.
   */
  def submitAll(
      controlCommands: Set[ControlCommand]
  )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext, mat: Materializer): Future[CommandResponse] = {
    val value = Source(controlCommands).mapAsyncUnordered(parallelism)(submit)
    CommandResponse.aggregateResponse(value)
  }

  /**
   * Send a command as a Oneway and get a [[csw.messages.ccs.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload
   * @return a CommandResponse as a Future value.
   */
  def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
    value ? (Oneway(controlCommand, _))

  /**
   * Subscribe for the result of a long running command which was sent as Submit to get a [[csw.messages.ccs.commands.CommandResponse]] as a Future.
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a Future value.
   */
  def subscribe(commandRunId: RunId)(implicit timeout: Timeout, scheduler: Scheduler): Future[CommandResponse] =
    value ? (CommandResponseManagerMessage.Subscribe(commandRunId, _))

  /**
   * Submit a command and Subscribe for the result if it was successfully validated as `Accepted` to get a final [[csw.messages.ccs.commands.CommandResponse]] as a Future.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @return a CommandResponse as a Future value.
   */
  def submitAndSubscribe(
      controlCommand: ControlCommand
  )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext): Future[CommandResponse] =
    submit(controlCommand).flatMap {
      case _: Accepted ⇒ subscribe(controlCommand.runId)
      case x           ⇒ Future.successful(x)
    }

  /**
   * Submit a command and match the published state from the component using a [[csw.messages.ccs.commands.StateMatcher]]. If the match is successful a `Completed` response is
   * provided as a future. In case of a failure or unmatched state, `Error` CommandResponse is provided as a Future.
   * @param controlCommand the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @param stateMatcher the StateMatcher implementation for matching received state against a demand state.
   * @return a CommandResponse as a Future value.
   */
  def submitAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher
  )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext, mat: Materializer): Future[CommandResponse] = {
    val matcher          = new Matcher(value, stateMatcher)
    val matcherResponseF = matcher.start
    submit(controlCommand).flatMap {
      case _: Accepted ⇒
        matcherResponseF.map {
          case MatchCompleted  ⇒ Completed(controlCommand.runId)
          case MatchFailed(ex) ⇒ Error(controlCommand.runId, ex.getMessage)
        }
      case x ⇒
        matcher.stop()
        Future.successful(x)
    }
  }

  /**
   * Submit multiple commands and get final CommandResponse for all as a stream of CommandResponse. For long running commands, it will subscribe for the
   * result of those which were successfully validated as `Accepted` and get the final CommandResponse.
   * @param controlCommands the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  def submitAllAndSubscribe(
      controlCommands: Set[ControlCommand]
  )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext): Source[CommandResponse, NotUsed] = {
    Source(controlCommands).mapAsyncUnordered(parallelism)(submitAndSubscribe)
  }

  /**
   * Submit multiple commands and get final CommandResponse for all as one CommandResponse. If all the commands were successful, a CommandResponse as
   * [[csw.messages.ccs.commands.CommandResponse.Completed]] will be returned. If any one of the command fails, an [[csw.messages.ccs.commands.CommandResponse.Error]]
   * will be returned. For long running commands, it will subscribe for the result of those which were successfully validated as `Accepted` and get the final CommandResponse.
   * @param controlCommands the [[csw.messages.ccs.commands.ControlCommand]] payload.
   * @return a CommandResponse as a Future value.
   */
  def submitAllAndSubscribe(
      controlCommands: Set[ControlCommand]
  )(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext, mat: Materializer): Future[CommandResponse] = {
    val value = Source(controlCommands).mapAsyncUnordered(parallelism)(submitAndSubscribe)
    CommandResponse.aggregateResponse(value)
  }
}
