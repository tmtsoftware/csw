package csw.services.location.scaladsl

import akka.typed.ActorRef
import csw.services.location.models.AkkaRegistration
import csw.services.location.models.Connection.AkkaConnection

class RegistrationFactory {
  def akkaTyped(akkaConnection: AkkaConnection, actorRef: ActorRef[_]): AkkaRegistration =
    AkkaRegistration(akkaConnection, actorRef)
}
