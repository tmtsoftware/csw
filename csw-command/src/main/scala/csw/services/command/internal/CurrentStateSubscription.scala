package csw.services.command.internal

import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, OverflowStrategy}
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.messages.ComponentCommonMessage.ComponentStateSubscription
import csw.messages.models.PubSub.Subscribe
import csw.messages.params.states.CurrentState

/**
 * The handle to the susbscription created for the current state published by the specified publisher
 * @param publisher the source of the current state
 * @param callback the action to perform on each received element
 * @param mat the materializer to materialize the underlying stream
 */
class CurrentStateSubscription private[command] (
    publisher: ActorRef[ComponentStateSubscription],
    callback: CurrentState ⇒ Unit
)(implicit val mat: Materializer) {

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
