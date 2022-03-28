/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api

import akka.actor.typed.ActorRef
import csw.location.api.commons.LocationServiceLogger
import csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaRegistration, Metadata}
import csw.logging.api.scaladsl.Logger

object AkkaRegistrationFactory extends AkkaRegistrationFactory

object JAkkaRegistrationFactory extends AkkaRegistrationFactory

/**
 * A factory to create AkkaRegistration
 */
class AkkaRegistrationFactory {

  /**
   * @param connection the `Connection` to register with `LocationService`
   * @param actorRef Provide a remote actor ref that is offering a connection. Local actors cannot be registered since they can't be
   *                 communicated from components across the network
   * @param metadata represents additional information associated with registration
   * @return AkkaRegistration instance. A [[csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed]]
   *        is thrown if the actorRefURI provided is not a remote actorRef uri
   */
  def make(connection: AkkaConnection, actorRef: ActorRef[_], metadata: Metadata): AkkaRegistration = {
    val actorRefURI = actorRef.toURI
    if (actorRefURI.getPort == -1) {
      val log: Logger            = LocationServiceLogger.getLogger
      val registrationNotAllowed = LocalAkkaActorRegistrationNotAllowed(actorRefURI)
      log.error(registrationNotAllowed.getMessage, ex = registrationNotAllowed)
      throw registrationNotAllowed
    }

    models.AkkaRegistration(connection, actorRefURI, metadata)
  }

  /**
   * @param connection the `Connection` to register with `LocationService`
   * @param actorRef Provide a remote actor ref that is offering a connection. Local actors cannot be registered since they can't be
   *                 communicated from components across the network
   * @return AkkaRegistration instance. A [[csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed]]
   *        is thrown if the actorRefURI provided is not a remote actorRef uri
   */
  def make(connection: AkkaConnection, actorRef: ActorRef[_]): AkkaRegistration =
    make(connection, actorRef, Metadata.empty)

}
