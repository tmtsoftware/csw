/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api

import org.apache.pekko.actor.typed.ActorRef
import csw.location.api.commons.LocationServiceLogger
import csw.location.api.exceptions.LocalPekkoActorRegistrationNotAllowed
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoRegistration, Metadata}
import csw.logging.api.scaladsl.Logger

object PekkoRegistrationFactory extends PekkoRegistrationFactory

object JPekkoRegistrationFactory extends PekkoRegistrationFactory

/**
 * A factory to create PekkoRegistration
 */
class PekkoRegistrationFactory {

  /**
   * @param connection the `Connection` to register with `LocationService`
   * @param actorRef Provide a remote actor ref that is offering a connection. Local actors cannot be registered since they can't be
   *                 communicated from components across the network
   * @param metadata represents additional information associated with registration
   * @return PekkoRegistration instance. A [[csw.location.api.exceptions.LocalPekkoActorRegistrationNotAllowed]]
   *        is thrown if the actorRefURI provided is not a remote actorRef uri
   */
  def make(connection: PekkoConnection, actorRef: ActorRef[_], metadata: Metadata): PekkoRegistration = {
    val actorRefURI = actorRef.toURI
    if (actorRefURI.getPort == -1) {
      val log: Logger            = LocationServiceLogger.getLogger
      val registrationNotAllowed = LocalPekkoActorRegistrationNotAllowed(actorRefURI)
      log.error(registrationNotAllowed.getMessage, ex = registrationNotAllowed)
      throw registrationNotAllowed
    }

    models.PekkoRegistration(connection, actorRefURI, metadata)
  }

  /**
   * @param connection the `Connection` to register with `LocationService`
   * @param actorRef Provide a remote actor ref that is offering a connection. Local actors cannot be registered since they can't be
   *                 communicated from components across the network
   * @return PekkoRegistration instance. A [[csw.location.api.exceptions.LocalPekkoActorRegistrationNotAllowed]]
   *        is thrown if the actorRefURI provided is not a remote actorRef uri
   */
  def make(connection: PekkoConnection, actorRef: ActorRef[_]): PekkoRegistration =
    make(connection, actorRef, Metadata.empty)

}
