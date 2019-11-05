package csw.command.client.internal

import java.util.concurrent.TimeoutException

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.typed.scaladsl
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{KillSwitches, Materializer, OverflowStrategy}
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.api.{CurrentStateSubscription, StateMatcher}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.messages.ComponentCommonMessage.ComponentStateSubscription
import csw.command.client.messages.{ComponentMessage, Query, QueryFinal}
import csw.command.client.models.framework.PubSub.{Subscribe, SubscribeOnly}
import csw.command.client.models.matchers.Matcher
import csw.command.client.models.matchers.MatcherResponses.{MatchCompleted, MatchFailed}
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

private[command] class CommandServiceImpl(componentLocation: AkkaLocation)(implicit val actorSystem: ActorSystem[_])
    extends CommandService {

  private implicit val ec: ExecutionContext = actorSystem.executionContext
  private implicit val mat: Materializer    = scaladsl.ActorMaterializer()
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  private val component: ActorRef[ComponentMessage] = componentLocation.componentRef
  private val ValidateTimeout                       = 1.seconds

  override def validate(controlCommand: ControlCommand): Future[ValidateResponse] = {
    implicit val timeout: Timeout = Timeout(ValidateTimeout)
    component ? (Validate(controlCommand, _))
  }

  def submitAndWait(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] = {
    val eventualResponse: Future[SubmitResponse] = component ? (Submit(controlCommand, _))
    eventualResponse.flatMap {
      case started: Started => queryFinal(started.runId)
      case x                => Future.successful(x)
    }
  }

  override def submit(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] =
    component ? (Submit(controlCommand, _))

  override def submitAllAndWait(
      submitCommands: List[ControlCommand]
  )(implicit timeout: Timeout): Future[List[SubmitResponse]] = {
    // This exception is used to pass the failing command response to the recover to shut down the stream
    class CommandFailureException(val r: SubmitResponse) extends Exception(r.toString)

    Source(submitCommands)
      .mapAsync(1)(submitAndWait)
      .map { response =>
        if (isNegative(response))
          throw new CommandFailureException(response)
        else
          response
      }
      .recover {
        // If the command fails, then terminate but return the last response giving the problem, others are ignored
        case ex: CommandFailureException => ex.r
      }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(_.toList)
  }

  override def oneway(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[OnewayResponse] =
    component ? (Oneway(controlCommand, _))

  override def onewayAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher
  )(implicit timeout: Timeout): Future[MatchingResponse] = {
    val matcher          = new Matcher(component, stateMatcher)
    val matcherResponseF = matcher.start
    oneway(controlCommand).flatMap {
      case Accepted(runId) =>
        matcherResponseF.map {
          case MatchCompleted  => Completed(runId)
          case MatchFailed(ex) => Error(runId, ex.getMessage)
        }
      case x @ _ =>
        matcher.stop()
        Future.successful(x.asInstanceOf[MatchingResponse])
    }
  }

  // components coming via this api will be removed from  subscriber's list after timeout
  def query(commandRunId: Id)(implicit timeout: Timeout): Future[QueryResponse] = {
    val eventualResponse: Future[QueryResponse] = component ? (Query(commandRunId, _))
    eventualResponse recover {
      case _: TimeoutException => CommandNotAvailable(commandRunId)
    }
  }

  // components coming via this api will be removed from  subscriber's list after timeout
  override def queryFinal(commandRunId: Id)(implicit timeout: Timeout): Future[SubmitResponse] =
    component ? (QueryFinal(commandRunId, _))

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.models.AkkaLocation]] of the component
   *
   * @param names subscribe to states which have any of the provided value for name.
   *              If no states are provided, subscription in made to all the states.
   * @return a CurrentStateSubscription to stop the subscription
   */
  override def subscribeCurrentState(names: Set[StateName]): Source[CurrentState, CurrentStateSubscription] = {
    val bufferSize = 256

    /*
     * Creates a stream of current state change of a component. An actorRef plays the source of the stream.
     * Whenever the stream will be materialized, the source actorRef will subscribe itself to CurrentState change of the target component.
     * Any change in current state of the target component will push the current state to source actorRef which will
     * then flow through the stream.
     */
    ActorSource
      .actorRef[CurrentState](
        completionMatcher = PartialFunction.empty,
        failureMatcher = PartialFunction.empty,
        bufferSize,
        overflowStrategy = OverflowStrategy.dropHead
      )
      .mapMaterializedValue { ref =>
        if (names.isEmpty) component ! ComponentStateSubscription(Subscribe(ref))
        else component ! ComponentStateSubscription(SubscribeOnly(ref, names))
      }
      .viaMat(KillSwitches.single)(Keep.right)
      .mapMaterializedValue(killSwitch => () => killSwitch.shutdown())
  }

  override def subscribeCurrentState(callback: CurrentState => Unit): CurrentStateSubscription =
    subscribeCurrentState().map(callback).toMat(Sink.ignore)(Keep.left).run()

  override def subscribeCurrentState(names: Set[StateName], callback: CurrentState => Unit): CurrentStateSubscription =
    subscribeCurrentState(names).map(callback).toMat(Sink.ignore)(Keep.left).run()

}
