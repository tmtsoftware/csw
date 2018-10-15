package csw.command.client.internal

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import csw.command.api.scaladsl.{CommandService, CurrentStateSubscription, StateMatcher}
import csw.command.client.internal.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.internal.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.internal.messages.{CommandResponseManagerMessage, ComponentMessage}
import csw.command.client.internal.models.matchers.Matcher
import csw.command.client.internal.models.matchers.MatcherResponses.{MatchCompleted, MatchFailed}
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.concurrent.{ExecutionContext, Future}

private[csw] class CommandServiceImpl(componentLocation: AkkaLocation)(implicit val actorSystem: ActorSystem[_])
    extends CommandService {

  private implicit val ec: ExecutionContext = actorSystem.executionContext
  private implicit val mat: Materializer    = ActorMaterializer()(actorSystem.toUntyped)
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  private val component: ActorRef[ComponentMessage] = componentLocation.componentRef

  def submit(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] =
    component ? (Submit(controlCommand, _))

  def completeAll(submitCommands: List[ControlCommand])(implicit timeout: Timeout): Future[List[SubmitResponse]] =
    Source(submitCommands)
      .mapAsync(1)(complete)
      .map { response =>
        if (isNegative(response))
          throw new RuntimeException(s"Command failed: $response")
        else
          response
      }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(_.toList)

  def queryFinal(commandRunId: Id)(implicit timeout: Timeout): Future[SubmitResponse] =
    component ? (CommandResponseManagerMessage.Subscribe(commandRunId, _))

  def complete(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] =
    submit(controlCommand).flatMap {
      case _: Started ⇒ queryFinal(controlCommand.runId)
      case x          ⇒ Future.successful(x)
    }

  def query(commandRunId: Id)(implicit timeout: Timeout): Future[QueryResponse] =
    component ? (CommandResponseManagerMessage.Query(commandRunId, _))

  def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[OnewayResponse] =
    component ? (Oneway(controlCommand, _))

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

  def validate(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[ValidateResponse] =
    component ? (Validate(controlCommand, _))

  def subscribeCurrentState(callback: CurrentState ⇒ Unit): CurrentStateSubscription =
    new CurrentStateSubscriptionImpl(component, None, callback)

  def subscribeCurrentState(names: Set[StateName], callback: CurrentState ⇒ Unit): CurrentStateSubscription =
    new CurrentStateSubscriptionImpl(component, Some(names), callback)

}
