package csw.services.ccs.internal

import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, OverflowStrategy}
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.messages.ComponentCommonMessage.ComponentStateSubscription
import csw.messages.models.PubSub.Subscribe
import csw.messages.params.states.CurrentState

class CurrentStateSubscription(publisher: ActorRef[ComponentStateSubscription], callback: CurrentState ⇒ Unit)(
    implicit val mat: Materializer
) {
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
    .toMat(Sink.head)(Keep.both)
    .run()

  def stop(): Unit = killSwitch.shutdown()
}
