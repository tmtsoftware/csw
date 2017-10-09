package csw.services.location.internal

import java.net.URI

import akka.typed.ActorRef
import csw.messages.location.AkkaLocation
import csw.messages.location.Connection.AkkaConnection

//FIXME decide the place of this factory once HttpLocation and TcpLocation requires similar factory
object AkkaLocationFactory {
  private[location] def make(connection: AkkaConnection, uri: URI, actorRef: ActorRef[_]) =
    AkkaLocation(connection, uri, actorRef, null)
}
