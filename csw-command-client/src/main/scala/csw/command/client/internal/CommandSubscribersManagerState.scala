package csw.command.client.internal

import akka.actor.typed.ActorRef
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.core.models.Id

/**
 * Manages subscribers state of a given command identified by a RunId
 *
 * @param cmdToSubscribers a map of runId to subscribers
 */
private[command] case class CommandSubscribersManagerState(cmdToSubscribers: Map[Id, Set[ActorRef[SubmitResponse]]]) {

  /**
   * Add a new subscriber for change in state
   *
   * @param runId command identifier
   * @param subscriber the subscriber as an actor to which the updated state will be sent
   * @return a new CommandSubscribersManagerState instance with updated subscribers
   */
  def subscribe(runId: Id, subscriber: ActorRef[SubmitResponse]): CommandSubscribersManagerState = {
    val updatedCmdToSubscribers: Map[Id, Set[ActorRef[SubmitResponse]]] = cmdToSubscribers.get(runId) match {
      case Some(subscribers) => cmdToSubscribers.updated(runId, subscribers + subscriber)
      case None              => cmdToSubscribers.updated(runId, Set(subscriber))
    }
    CommandSubscribersManagerState(cmdToSubscribers = updatedCmdToSubscribers)
  }

  /**
   * Remove a subscriber for change in state
   *
   * @param runId command identifier
   * @param subscriber the subscriber as an actor to which the updated state will be sent
   * @return a new CommandSubscribersManagerState instance with updated subscribers
   */
  def unSubscribe(runId: Id, subscriber: ActorRef[SubmitResponse]): CommandSubscribersManagerState = {
    val updatedCmdToSubscribers: Map[Id, Set[ActorRef[SubmitResponse]]] = cmdToSubscribers.get(runId) match {
      case Some(subscribers) => cmdToSubscribers.updated(runId, subscribers - subscriber)
      case None              => cmdToSubscribers
    }
    CommandSubscribersManagerState(cmdToSubscribers = updatedCmdToSubscribers)
  }

  /**
   * Get the subscribers for the command
   *
   * @param runId command identifier
   * @return current command response
   */
  def getSubscribers(runId: Id): Set[ActorRef[SubmitResponse]] = cmdToSubscribers.get(runId) match {
    case Some(subscribers) => subscribers
    case None              => Set.empty
  }

  def removeSubscriber(actorRef: ActorRef[SubmitResponse]): CommandSubscribersManagerState = {
    def remove(ids: List[Id], commandSubscribersManagerState: CommandSubscribersManagerState): CommandSubscribersManagerState = {
      ids match {
        case Nil                ⇒ commandSubscribersManagerState
        case id :: remainingIds ⇒ remove(remainingIds, unSubscribe(id, actorRef))
      }
    }
    remove(cmdToSubscribers.keys.toList, this)
  }
}
