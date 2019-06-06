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
 * The handle to the subscription created for the current state published by the specified publisher
 *
 * @param publisher the source of the current state
 * @param callback the action to perform on each received element
 * @param mat the materializer to materialize the underlying stream
 */
private[internal] class CommandUpdateSubscriptionImpl(
    publisher: ActorRef[StartedCommandSubscription],
    names: Option[Set[StateName]],
    callback: SubmitResponse ⇒ Unit
)(implicit val mat: Materializer)
    extends CommandUpdateSubscription {

  /**
   * Create a stream of current state change of a component. An actorRef plays the source of the stream. When stream starts
   * running (materialized) the source actorRef subscribes itself to CurrentState change of the target component. Any change in
   * current state of target component will push the current state to source actorRef and this will flow though the stream
   * to sink. The callback provided by component developers is executed for the current state flowing through the stream.
   */
  private def source: Source[SubmitResponse, Unit] = {
    val bufferSize = 256
    Source
      .actorRef[SubmitResponse](bufferSize, OverflowStrategy.dropHead)
      .mapMaterializedValue { ref ⇒
        names match {
          case Some(value) => publisher ! StartedCommandSubscription(SubscribeOnly(ref, value))
          case None => {
            println(s"Got Started command subsc for: $ref - publisher: $publisher")
            publisher ! StartedCommandSubscription(Subscribe(ref))
          }
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
