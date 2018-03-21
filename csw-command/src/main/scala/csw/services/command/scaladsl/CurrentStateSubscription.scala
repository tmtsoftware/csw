package csw.services.command.scaladsl

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, OverflowStrategy}
import csw.messages.framework.PubSub.Subscribe
import csw.messages.params.states.CurrentState
import csw.messages.scaladsl.ComponentCommonMessage.ComponentStateSubscription

/**
 * The handle to the subscription created for the current state published by the specified publisher
 *
 * @param publisher the source of the current state
 * @param callback the action to perform on each received element
 * @param mat the materializer to materialize the underlying stream
 */
class CurrentStateSubscription private[command] (
    publisher: ActorRef[ComponentStateSubscription],
    callback: CurrentState ⇒ Unit
)(implicit val mat: Materializer) {

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
        publisher ! ComponentStateSubscription(Subscribe(ref))
      }
  }

  private val (killSwitch, currentStateF) = source
    .map(callback)
    .viaMat(KillSwitches.single)(Keep.right)
    .toMat(Sink.ignore)(Keep.both)
    .run()

  /**
   * Unsubscribe to the current state being published
   */
  def unsubscribe(): Unit = killSwitch.shutdown()
}
