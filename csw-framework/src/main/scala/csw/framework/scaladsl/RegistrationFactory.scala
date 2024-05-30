/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.scaladsl

import org.apache.pekko.actor.typed.ActorRef
import csw.location.api.PekkoRegistrationFactory
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoRegistration, Metadata}

/**
 * `RegistrationFactory` helps creating an PekkoRegistration. It is currently used by `csw-framework` to register different components on jvm boot-up.
 */
class RegistrationFactory {

  /**
   * Creates an PekkoRegistration from provided parameters. Currently, it is used to register components except Container.
   * A [[csw.location.api.exceptions.LocalPekkoActorRegistrationNotAllowed]] can be thrown if the actorRef provided
   * is not a remote actorRef.
   *
   * @param pekkoConnection the PekkoConnection representing the component
   * @param actorRef the supervisor actorRef of the component
   * @param metadata represents additional information associated with registration. If not provided, defaulted to empty value
   * @return a handle to the PekkoRegistration that is used to register in location service
   */
  def pekkoTyped(
      pekkoConnection: PekkoConnection,
      actorRef: ActorRef[?],
      metadata: Metadata = Metadata.empty
  ): PekkoRegistration =
    PekkoRegistrationFactory.make(pekkoConnection, actorRef, metadata)
}
