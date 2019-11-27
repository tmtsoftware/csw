package csw.command.client.internal

import java.util.concurrent.TimeoutException

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{KillSwitches, OverflowStrategy}
import akka.util.Timeout
import csw.command.api.StateMatcher
import csw.command.api.scaladsl.CommandService
import csw.command.api.utils.CommandServiceExtension
import csw.command.client.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.messages.ComponentCommonMessage.ComponentStateSubscription
import csw.command.client.messages.{ComponentMessage, Query, QueryFinal}
import csw.command.client.models.framework.PubSub.{Subscribe, SubscribeOnly}
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import msocket.api.models.Subscription

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

private[command] class CommandServiceImpl(component: ActorRef[ComponentMessage])(implicit val actorSystem: ActorSystem[_])
    extends CommandService {

  private implicit val ec: ExecutionContext = actorSystem.executionContext

  private val extension = new CommandServiceExtension(this)

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def validate(controlCommand: ControlCommand): Future[ValidateResponse] = {
    component ? (Validate(controlCommand, _))
  }

  def submitAndWait(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] =
    extension.submitAndWait(controlCommand)

  override def submit(controlCommand: ControlCommand): Future[SubmitResponse] =
    component ? (Submit(controlCommand, _))

  override def submitAllAndWait(submitCommands: List[ControlCommand])(implicit timeout: Timeout): Future[List[SubmitResponse]] =
    extension.submitAllAndWait(submitCommands)

  override def oneway(controlCommand: ControlCommand): Future[OnewayResponse] =
    component ? (Oneway(controlCommand, _))

  override def onewayAndMatch(controlCommand: ControlCommand, stateMatcher: StateMatcher): Future[MatchingResponse] =
    extension.onewayAndMatch(controlCommand, stateMatcher)

  // components coming via this api will be removed from  subscriber's list after timeout
  override def query(commandRunId: Id): Future[QueryResponse] = {
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
  override def subscribeCurrentState(names: Set[StateName]): Source[CurrentState, Subscription] = {
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

  override def subscribeCurrentState(callback: CurrentState => Unit): Subscription =
    subscribeCurrentState().map(callback).toMat(Sink.ignore)(Keep.left).run()

  override def subscribeCurrentState(names: Set[StateName], callback: CurrentState => Unit): Subscription =
    subscribeCurrentState(names).map(callback).toMat(Sink.ignore)(Keep.left).run()

}
