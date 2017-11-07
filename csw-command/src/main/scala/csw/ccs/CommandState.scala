package csw.ccs

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId

case class CommandState(
    runId: RunId,
    replyTo: Option[ActorRef[CommandResponse]],
    subscribers: Set[ActorRef[CommandResponse]],
    currentCmdStatus: CommandResponse
) {
  def sendStatus(): Unit    = replyTo.foreach(_ ! currentCmdStatus)
  def publishStatus(): Unit = subscribers.foreach(_ ! currentCmdStatus)
}
