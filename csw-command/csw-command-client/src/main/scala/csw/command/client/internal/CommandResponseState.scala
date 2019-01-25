package csw.command.client.internal
import com.github.benmanes.caffeine.cache.Cache
import csw.params.commands.CommandResponse.{CommandNotAvailable, QueryResponse, SubmitResponse}
import csw.params.core.models.Id

/**
 * Manages state of a given command identified by a RunId
 *
 * @param cmdToCmdResponse a cache having runIds mapped to their CommandState
 */
private[command] class CommandResponseState(private[command] val cmdToCmdResponse: Cache[Id, SubmitResponse]) {

  /**
   * Add the command with some initial response
   *
   * @param runId command identifier
   * @param initialResponse an initial response
   */
  def add(runId: Id, initialResponse: SubmitResponse): Unit = cmdToCmdResponse.put(runId, initialResponse)

  /**
   * Get the current command response for the command
   *
   * @param runId command identifier
   * @return current command response
   */
  def get(runId: Id): QueryResponse = cmdToCmdResponse.getIfPresent(runId) match {
    case null        => CommandNotAvailable(runId)
    case cmdResponse => cmdResponse
  }

  /**
   * Update the current command response for the command
   *
   * @param commandResponse the command response to be updated for this command
   */
  def updateCommandStatus(commandResponse: SubmitResponse): Unit =
    cmdToCmdResponse.getIfPresent(commandResponse.runId) match {
      case null ⇒
      case _    ⇒ cmdToCmdResponse.put(commandResponse.runId, commandResponse)
    }
}

case class CommandResponseReadOnlyState(cmdToCmdResponse: Map[Id, SubmitResponse])
