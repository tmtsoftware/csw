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
  def get(runId: Id): QueryResponse = cachedResponse(runId) match {
    case Some(cmdResponse) => cmdResponse
    case None              => CommandNotAvailable(runId)
  }

  /**
   * Update the current command response for the command
   *
   * @param commandResponse the command response to be updated for this command
   */
  def updateCommandStatus(commandResponse: SubmitResponse): Unit =
    cachedResponse(commandResponse.runId) match {
      case Some(_) ⇒ cmdToCmdResponse.put(commandResponse.runId, commandResponse)
      case None    ⇒
    }

  def asMap: Map[Id, SubmitResponse] = cmdToCmdResponse.asMap().asScala.toMap

  private def cachedResponse(id: Id) = Option(cmdToCmdResponse.getIfPresent(id))
}
