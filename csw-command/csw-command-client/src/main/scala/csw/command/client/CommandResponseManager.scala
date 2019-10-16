package csw.command.client

import akka.actor.typed.ActorRef
import com.github.benmanes.caffeine.cache.{Cache, Caffeine, RemovalCause}
import csw.params.commands.{CommandName, CommandResponse}
import csw.params.commands.CommandResponse.{CommandNotAvailable, QueryResponse, Started, SubmitResponse}
import csw.params.core.models.Id

import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

/**
 * Wrapper API for interacting with Command Response Manager of a component
 */
class CommandResponseManager(maxSize: Int) {
  case class CRMState(response: SubmitResponse, subscribers: Set[ActorRef[QueryResponse]] = Set.empty)

  private val cache: Cache[Id, CRMState] = Caffeine
    .newBuilder()
    .maximumSize(maxSize)
    .removalListener((k: Id, v: CRMState, _: RemovalCause) => {
      //todo: log a warning
      v.subscribers.foreach(_ ! CommandResponse.Error(v.response.commandName, k, "too many commands"))
    })
    .build()

  private[csw] def queryFinal(runId: Id, replyTo: ActorRef[QueryResponse]): Unit = {

    val currentStateMaybe = Option(cache.getIfPresent(runId))
    currentStateMaybe match {
      case Some(state @ CRMState(_: Started, _)) =>
        cache.put(runId, state.copy(subscribers = state.subscribers + replyTo))
      case Some(CRMState(finalResponse, _)) => replyTo ! finalResponse
      case None                             => replyTo ! CommandNotAvailable(CommandName("CommandNotAvailable"), runId)
    }
  }

  private[csw] def query(runId: Id, replyTo: ActorRef[QueryResponse]): Unit = {
    val currentStateMaybe = Option(cache.getIfPresent(runId))
    currentStateMaybe match {
      case Some(CRMState(response, _)) => replyTo ! response
      case None                        => replyTo ! CommandNotAvailable(CommandName("CommandNotAvailable"), runId)
    }
  }

  private[csw] def getState = cache.asMap().asScala.toMap

  /**
   * Add a new command or update an existing command with the provided status
   *
   * @param submitResponse final update for a started command [[csw.params.commands.CommandResponse.SubmitResponse]]
   */

  // fixme: update to subscribers not being done. can be done in cache update key as well
  def updateCommand(submitResponse: SubmitResponse): Unit = cache.put(submitResponse.runId, CRMState(submitResponse))

}
