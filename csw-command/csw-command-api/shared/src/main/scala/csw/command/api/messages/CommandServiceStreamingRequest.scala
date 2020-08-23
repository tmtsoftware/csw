package csw.command.api.messages

import akka.util.Timeout
import csw.params.core.models.Id
import csw.params.core.states.StateName

sealed trait CommandServiceStreamingRequest

object CommandServiceStreamingRequest {
  case class QueryFinal(runId: Id, timeoutInSeconds: Timeout)         extends CommandServiceStreamingRequest
  case class SubscribeCurrentState(names: Set[StateName] = Set.empty) extends CommandServiceStreamingRequest
}
