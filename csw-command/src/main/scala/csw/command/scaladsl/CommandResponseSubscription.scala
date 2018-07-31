package csw.command.scaladsl

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, OverflowStrategy}
import csw.command.messages.CommandResponseManagerMessage
import csw.command.messages.CommandResponseManagerMessage.Subscribe
import csw.params.commands.CommandResponse
import csw.params.core.models.Id
import csw.messages.CommandResponseManagerMessage
import csw.messages.params.models.Id
import csw.messages.CommandResponseManagerMessage.Subscribe
import csw.messages.commands.CommandResponse.SubmitResponse

/**
 * The handle to the subscription created for the current state published by the specified publisher
 *
 * @param runId the source of the current state
 * @param callback the action to perform on each received element
 * @param mat the materializer to materialize the underlying stream
 */
class CommandResponseSubscription private[csw] (
    runId: Id,
    commandResponseManagerActor: ActorRef[CommandResponseManagerMessage],
    callback: SubmitResponse ⇒ Unit
)(implicit val mat: Materializer) {

  /**
   * Create a stream of status of a command running on some component. An actorRef plays the source of the stream. When
   * stream starts running (materialized) the source actorRef subscribes itself to command status of the target component.
   * Any change in status of the command will push the new status to source actorRef and this will flow though the stream
   * to sink. The callback provided by component developers is executed for the status flowing through the stream.
   */
  private def source: Source[SubmitResponse, Unit] = {
    val bufferSize = 256
    Source
      .actorRef[SubmitResponse](bufferSize, OverflowStrategy.dropHead)
      .mapMaterializedValue { ref ⇒
        commandResponseManagerActor ! Subscribe(runId, ref)
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
  def unsubscribe(): Unit = killSwitch.shutdown()
}
