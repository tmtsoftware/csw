package csw.command.client.internal

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, OverflowStrategy}
import csw.command.api.CurrentStateSubscription
import csw.command.client.internal.messages.ComponentCommonMessage.ComponentStateSubscription
import csw.command.client.internal.models.framework.PubSub.{Subscribe, SubscribeOnly}
import csw.params.core.states.{CurrentState, StateName}

/**
 * The handle to the subscription created for the current state published by the specified publisher
 *
 * @param publisher the source of the current state
 * @param callback the action to perform on each received element
 * @param mat the materializer to materialize the underlying stream
 */
class CurrentStateSubscriptionImpl private[command] (
    publisher: ActorRef[ComponentStateSubscription],
    names: Option[Set[StateName]],
    callback: CurrentState ⇒ Unit
)(implicit val mat: Materializer)
    extends CurrentStateSubscription {

  /**
   * Create a stream of current state change of a component. An actorRef plays the source of the stream. When stream starts
   * running (materialized) the source actorRef subscribes itself to CurrentState change of the target component. Any change in
   * current state of target component will push the current state to source actorRef and this will flow though the stream
   * to sink. The callback provided by component developers is executed for the current state flowing through the stream.
   */
  private def source: Source[CurrentState, Unit] = {
    val bufferSize = 256
    Source
      .actorRef[CurrentState](bufferSize, OverflowStrategy.dropHead)
      .mapMaterializedValue { ref ⇒
        names match {
          case Some(value) => publisher ! ComponentStateSubscription(SubscribeOnly(ref, value))
          case None        => publisher ! ComponentStateSubscription(Subscribe(ref))
        }
      }
  }

  private val (killSwitch, _) = source
    .map(callback)
    .viaMat(KillSwitches.single)(Keep.right)
    .toMat(Sink.ignore)(Keep.both)
    .run()

  /**
   * Unsubscribe to the current state being published
   */
  override def unsubscribe(): Unit = killSwitch.shutdown()
}
