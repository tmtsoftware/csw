/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.exceptions

import java.net.URI

/**
 * An Exception representing failure in registering non remote actors
 *
 * @param actorRefURI the reference of the Actor that is expected to be remote but instead it is local
 */
case class LocalAkkaActorRegistrationNotAllowed(actorRefURI: URI)
    extends RuntimeException(s"Registration of only remote actors is allowed. Instead local actor $actorRefURI received.")

/**
 * Represents the current node is not able to join the cluster
 */
case class CouldNotJoinCluster() extends RuntimeException("could not join cluster")
