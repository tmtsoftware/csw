package csw.services.location.internal

import akka.typed.ActorRef
import csw.messages.location.Connection.AkkaConnection
import csw.services.location.models.AkkaRegistration

//FIXME decide the place of this factory once HttpRegistration and TcpRegistration requires similar factory
object AkkaRegistrationFactory {
  private[location] def make(connection: AkkaConnection, actorRef: ActorRef[_]) =
    AkkaRegistration(connection, actorRef, null)
}
