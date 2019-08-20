package csw.command.client.internal

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, OverflowStrategy}
import csw.command.api.CommandUpdateSubscription
import csw.command.client.messages.ComponentCommonMessage.StartedCommandSubscription
import csw.command.client.models.framework.PubSub.{Subscribe, SubscribeOnly}
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.core.states.StateName

/**
 * The handle to the subscription created for the command updates published by the specified publisher
 *
 * @param publisher the source of the command updates
 * @param callback the action to perform on each received element
 * @param mat the materializer to materialize the underlying stream
 */
private[internal] class CommandUpdateSubscriptionImpl(
    publisher: ActorRef[StartedCommandSubscription],
    names: Option[Set[StateName]],
    callback: SubmitResponse => Unit
)(implicit val mat: Materializer)
    extends CommandUpdateSubscription {

  /**
   * Create a stream of command updates to a command service. An actorRef plays the source of the stream. When stream starts
   * running (materialized) the source actorRef subscribes itself to command updates from the target component. Any published
   * SubmitResponse will push to source actorRef and this will flow though the stream
   * to sink. The callback provided by command service impl is executed for the SubmitResponse flowing through the stream.
   */
  private def source: Source[SubmitResponse, Unit] = {
    val bufferSize = 256
    Source
      .actorRef[SubmitResponse](bufferSize, OverflowStrategy.dropHead)
      .mapMaterializedValue { ref =>
        names match {
          case Some(value) =>
            publisher ! StartedCommandSubscription(SubscribeOnly(ref, value))
          case None =>
            publisher ! StartedCommandSubscription(Subscribe(ref))
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
  override def unsubscribe(): Unit = {
    killSwitch.shutdown()
  }
}
