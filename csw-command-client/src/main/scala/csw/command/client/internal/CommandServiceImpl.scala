package csw.command.client.internal

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.api.{CurrentStateSubscription, StateMatcher}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.messages.{CommandResponseManagerMessage, ComponentMessage}
import csw.command.client.models.matchers.Matcher
import csw.command.client.models.matchers.MatcherResponses.{MatchCompleted, MatchFailed}
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.concurrent.{ExecutionContext, Future}

private[command] class CommandServiceImpl(componentLocation: AkkaLocation)(implicit val actorSystem: ActorSystem[_])
    extends CommandService {

  private implicit val ec: ExecutionContext = actorSystem.executionContext
  private implicit val mat: Materializer    = ActorMaterializer()(actorSystem.toUntyped)
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  private val component: ActorRef[ComponentMessage] = componentLocation.componentRef

  override def validate(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[ValidateResponse] =
    component ? (Validate(controlCommand, _))

  override def submit(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] = {
    val eventualResponse: Future[SubmitResponse] = component ? (Submit(controlCommand, _))
    eventualResponse.flatMap {
      case _: Started ⇒ component ? (CommandResponseManagerMessage.Subscribe(controlCommand.runId, _))
      case x          ⇒ Future.successful(x)
    }
  }

  override def submitAll(
      submitCommands: List[ControlCommand]
  )(implicit timeout: Timeout): Future[List[SubmitResponse]] =
    Source(submitCommands)
      .mapAsync(1)(submit)
      .map { response =>
        if (isNegative(response))
          throw new RuntimeException(s"Command failed: $response")
        else
          response
      }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(_.toList)

  override def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[OnewayResponse] =
    component ? (Oneway(controlCommand, _))

  override def onewayAndMatch(
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

  override def query(commandRunId: Id)(implicit timeout: Timeout): Future[QueryResponse] =
    component ? (CommandResponseManagerMessage.Query(commandRunId, _))

  override def subscribeCurrentState(callback: CurrentState ⇒ Unit): CurrentStateSubscription =
    new CurrentStateSubscriptionImpl(component, None, callback)

  override def subscribeCurrentState(names: Set[StateName], callback: CurrentState ⇒ Unit): CurrentStateSubscription =
    new CurrentStateSubscriptionImpl(component, Some(names), callback)

}
