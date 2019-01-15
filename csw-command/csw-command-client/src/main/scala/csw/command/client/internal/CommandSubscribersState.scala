package csw.command.client.internal

import akka.actor.typed.ActorRef
import csw.command.client.Store
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.core.models.Id

/**
 * Manages subscribers state of a given command identified by a RunId
 */
private[command] case class CommandSubscribersState(store: Store[Id, ActorRef[SubmitResponse]])
