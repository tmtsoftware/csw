package csw.services.ccs.internal

import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, OverflowStrategy}
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.messages.CommandResponseManagerMessage
import csw.messages.CommandResponseManagerMessage.Subscribe
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.Id

/**
 * The handle to the subscription created for the current state published by the specified publisher
 * @param runId the source of the current state
 * @param callback the action to perform on each received element
 * @param mat the materializer to materialize the underlying stream
 */
class CommandResponseSubscription(
    runId: Id,
    commandResponseManagerActor: ActorRef[CommandResponseManagerMessage],
    callback: CommandResponse ⇒ Unit
)(implicit val mat: Materializer) {

  private def source: Source[CommandResponse, Unit] = {
    val bufferSize = 256
    Source
      .actorRef[CommandResponse](bufferSize, OverflowStrategy.dropHead)
      .mapMaterializedValue { ref ⇒
        commandResponseManagerActor ! Subscribe(runId, ref)
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
