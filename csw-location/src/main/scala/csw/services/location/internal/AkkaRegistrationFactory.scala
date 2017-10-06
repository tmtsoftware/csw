package csw.services.location.internal

import akka.typed.ActorRef
import csw.messages.location.Connection.AkkaConnection
import csw.services.location.models.AkkaRegistration

/**
 * It is strictly recommended to use for testing purposes only
 */
object AkkaRegistrationFactory {
  def make(connection: AkkaConnection, actorRef: ActorRef[_]) = AkkaRegistration(connection, actorRef, null)
}
