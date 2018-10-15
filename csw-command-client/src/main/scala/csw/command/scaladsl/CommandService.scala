package csw.command.scaladsl

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import csw.command.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.messages.{CommandResponseManagerMessage, ComponentMessage}
import csw.command.models.matchers.MatcherResponses.{MatchCompleted, MatchFailed}
import csw.command.models.matchers.{Matcher, StateMatcher}
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.concurrent.{ExecutionContext, Future}

/**
 * A Command Service API of a csw component. This model provides method based APIs for command interactions with a component.
 *
 * @param componentLocation [[csw.location.api.models.AkkaLocation]] of the component
 */
class CommandService(componentLocation: AkkaLocation)(implicit val actorSystem: ActorSystem[_]) {

  private implicit val ec: ExecutionContext = actorSystem.executionContext
  private implicit val mat: Materializer    = ActorMaterializer()(actorSystem.toUntyped)
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  private val component: ActorRef[ComponentMessage] = componentLocation.componentRef

  /**
   * Submit a command and get a [[csw.params.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse
   * can be obtained by using `subscribe` API.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a CommandResponse as a Future value
   */
  def submit(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] =
    component ? (Submit(controlCommand, _))

  /**
   * Submit multiple commands and get a Source of [[csw.params.commands.CommandResponse]] for all commands. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse can be obtained by using `subscribe` API.
   *
   * @param submitCommands the set of [[csw.params.commands.ControlCommand]] payloads
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  def submitAll(submitCommands: List[ControlCommand])(implicit timeout: Timeout): Future[List[SubmitResponse]] =
    Source(submitCommands)
      .mapAsync(1)(submitAndGetFinalResponse)
      .map { response =>
        if (isNegative(response))
          throw new RuntimeException(s"Command failed: $response")
        else
          response
      }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(_.toList)

  /**
   * Subscribe for the result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse]] as a Future
   *
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a Future value
   */
  def getFinalResponse(commandRunId: Id)(implicit timeout: Timeout): Future[SubmitResponse] =
    component ? (CommandResponseManagerMessage.Subscribe(commandRunId, _))

  /**
   * Submit a command and Subscribe for the result if it was successfully validated as `Started` to get a
   * final [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a CommandResponse as a Future value
   */
  def submitAndGetFinalResponse(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] =
    submit(controlCommand).flatMap {
      case _: Started ⇒ getFinalResponse(controlCommand.runId)
      case x          ⇒ Future.successful(x)
    }

  /**
   * Query for the result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse]] as a Future
   *
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a Future value
   */
  def query(commandRunId: Id)(implicit timeout: Timeout): Future[QueryResponse] =
    component ? (CommandResponseManagerMessage.Query(commandRunId, _))

  /**
   * Send a command as a Oneway and get a [[csw.params.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a CommandResponse as a Future value
   */
  def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[OnewayResponse] =
    component ? (Oneway(controlCommand, _))

  /**
   * Submit a command and match the published state from the component using a [[csw.command.models.matchers.StateMatcher]].
   * If the match is successful a `Completed` response is
   * provided as a future. In case of a failure or unmatched state, `Error` CommandResponse is provided as a Future.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @param stateMatcher the StateMatcher implementation for matching received state against a demand state
   * @return a MatchingResponse as a Future value
   */
  def onewayAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher
  )(implicit timeout: Timeout): Future[MatchingResponse] = {
    val matcher          = new Matcher(component, stateMatcher)
    val matcherResponseF = matcher.start
    oneway(controlCommand).flatMap {
      case Accepted(runId) ⇒
        matcherResponseF.map {
          case MatchCompleted  ⇒ Completed(runId)
          case MatchFailed(ex) ⇒ Error(runId, ex.getMessage)
        }
      case x @ _ =>
        matcher.stop()
        Future.successful(x.asInstanceOf[MatchingResponse])
    }
  }

  /**
   * Send a Validate command and get ValidateResponse as a Future. The ValidateResponse can be of type Accepted, Invalid
   * or Locked.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a ValidateResponse as a Future value
   */
  def validate(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[ValidateResponse] =
    component ? (Validate(controlCommand, _))

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.api.models.AkkaLocation]] of the component
   *
   * @param callback the action to be applied on the CurrentState element received as a result of subscription
   * @return a CurrentStateSubscription to stop the subscription
   */
  def subscribeCurrentState(callback: CurrentState ⇒ Unit): CurrentStateSubscription =
    new CurrentStateSubscription(component, None, callback)

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.api.models.AkkaLocation]] of the component
   *
   * @param names subscribe to only those states which have any of the the provided value for name
   * @param callback the action to be applied on the CurrentState element received as a result of subscription
   * @return a CurrentStateSubscription to stop the subscription
   */
  def subscribeOnlyCurrentState(names: Set[StateName], callback: CurrentState ⇒ Unit): CurrentStateSubscription =
    new CurrentStateSubscription(component, Some(names), callback)
}
