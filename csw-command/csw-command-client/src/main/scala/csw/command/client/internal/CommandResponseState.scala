package csw.command.client.internal
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import csw.params.commands.CommandResponse.{CommandNotAvailable, QueryResponse, SubmitResponse}
import csw.params.core.models.Id

import scala.collection.JavaConverters.mapAsScalaConcurrentMapConverter

// Cache command for certain duration and evict them once the expiration happens
private[command] class CommandResponseState(crmCacheProperties: CRMCacheProperties) {

  // older messages are evicted based on duration
  private val cmdToCmdResponse: Cache[Id, SubmitResponse] = Caffeine
    .newBuilder()
    .maximumSize(crmCacheProperties.maxSize)
    .expireAfterWrite(crmCacheProperties.expiry)
    .build()

  // initialResponse will be Started
  def add(initialResponse: SubmitResponse): Unit = cmdToCmdResponse.put(initialResponse.runId, initialResponse)
  def update(commandResponse: SubmitResponse): Unit =
    cachedResponse(commandResponse.runId) match {
      case Some(_) ⇒ cmdToCmdResponse.put(commandResponse.runId, commandResponse)
      case None    ⇒
    }
  def get(runId: Id): QueryResponse  = cachedResponse(runId).getOrElse(CommandNotAvailable(runId))
  def asMap: Map[Id, SubmitResponse] = cmdToCmdResponse.asMap().asScala.toMap

  private def cachedResponse(id: Id): Option[SubmitResponse] = Option(cmdToCmdResponse.getIfPresent(id))
}
