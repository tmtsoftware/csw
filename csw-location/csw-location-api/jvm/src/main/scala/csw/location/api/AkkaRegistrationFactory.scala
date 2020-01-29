package csw.location.api

import java.net.URI

import csw.location.api.commons.LocationServiceLogger
import csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.location.api.models.AkkaRegistration
import csw.location.api.models.Connection.AkkaConnection
import csw.logging.api.scaladsl.Logger

/**
 * A factory to create AkkaRegistration
 */
object AkkaRegistrationFactory {

  /**
   *
   * @param connection the `Connection` to register with `LocationService`
\   * @param actorRefURI Provide a remote actor uri that is offering a connection. Local actors cannot be registered since they can't be
   *                 communicated from components across the network
   * @return AkkaRegistration instance. A [[csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed]]
   *        is thrown if the actorRefURI provided is not a remote actorRef uri
   */
  def make(connection: AkkaConnection, actorRefURI: URI): AkkaRegistration = {
    if (actorRefURI.getPort == -1) {
      val log: Logger            = LocationServiceLogger.getLogger
      val registrationNotAllowed = LocalAkkaActorRegistrationNotAllowed(actorRefURI)
      log.error(registrationNotAllowed.getMessage, ex = registrationNotAllowed)
      throw registrationNotAllowed
    }

    models.AkkaRegistration(connection, actorRefURI)
  }
}
