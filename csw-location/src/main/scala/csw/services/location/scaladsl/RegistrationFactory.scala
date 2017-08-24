package csw.services.location.scaladsl

import akka.typed
import akka.typed.scaladsl.adapter.TypedActorRefOps
import csw.services.location.models.AkkaRegistration
import csw.services.location.models.Connection.AkkaConnection

class RegistrationFactory {
  def akkaTyped(akkaConnection: AkkaConnection, actorRef: typed.ActorRef[_]): AkkaRegistration =
    AkkaRegistration(akkaConnection, actorRef.toUntyped)
}
