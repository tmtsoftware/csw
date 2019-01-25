package csw.command.client.internal
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import csw.params.commands.CommandResponse.{CommandNotAvailable, QueryResponse, SubmitResponse}
import csw.params.core.models.Id

import scala.collection.JavaConverters.mapAsScalaConcurrentMapConverter

/**
 * Manages state of a given command identified by a RunId
 *
 * @param crmCacheProperties CRM cache properties
 */
private[command] class CommandResponseState(crmCacheProperties: CRMCacheProperties) {

  private val cmdToCmdResponse: Cache[Id, SubmitResponse] = Caffeine
    .newBuilder()
    .maximumSize(crmCacheProperties.maxSize)
    .expireAfterWrite(crmCacheProperties.expiry)
    .build()

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

  def state: Map[Id, SubmitResponse] = cmdToCmdResponse.asMap().asScala.toMap
}
