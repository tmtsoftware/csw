package csw.location.api

import java.net.URI

import csw.location.api.commons.LocationServiceLogger
import csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.location.model.scaladsl.AkkaRegistration
import csw.location.model.scaladsl.Connection.AkkaConnection
import csw.logging.api.scaladsl.Logger
import csw.params.core.models.Prefix

object RegistrationFactory {
  //    A [[csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed]]
  //     * is thrown if the actorRef provided is not a remote actorRef
  def akkaRegistration(connection: AkkaConnection, prefix: Prefix, actorRefURI: URI): AkkaRegistration = {
    if (actorRefURI.getPort == -1) {
      val log: Logger            = LocationServiceLogger.getLogger
      val registrationNotAllowed = LocalAkkaActorRegistrationNotAllowed(actorRefURI)
      log.error(registrationNotAllowed.getMessage, ex = registrationNotAllowed)
      throw registrationNotAllowed
    }

    AkkaRegistration(connection, prefix, actorRefURI)
  }
}
