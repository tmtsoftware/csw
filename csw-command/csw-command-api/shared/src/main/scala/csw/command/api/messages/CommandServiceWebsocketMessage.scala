package csw.command.api.messages

import akka.util.Timeout
import csw.params.core.models.Id
import csw.params.core.states.StateName

sealed trait CommandServiceWebsocketMessage

object CommandServiceWebsocketMessage {
  case class QueryFinal(runId: Id, timeoutInSeconds: Timeout)         extends CommandServiceWebsocketMessage
  case class SubscribeCurrentState(names: Set[StateName] = Set.empty) extends CommandServiceWebsocketMessage
}
