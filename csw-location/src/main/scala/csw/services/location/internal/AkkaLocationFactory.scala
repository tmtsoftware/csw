package csw.services.location.internal

import java.net.URI

import akka.typed.ActorRef
import csw.messages.location.AkkaLocation
import csw.messages.location.Connection.AkkaConnection

/**
 * It is strictly recommended to use for testing purposes only
 */
object AkkaLocationFactory {
  private[location] def make(connection: AkkaConnection, uri: URI, actorRef: ActorRef[_]) =
    AkkaLocation(connection, uri, actorRef, null)
}
