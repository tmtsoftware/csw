/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.extensions

import java.net.URI

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.adapter.TypedActorRefOps
import org.apache.pekko.serialization.Serialization

object ActorExtension {
  implicit class RichActor(actorRef: ActorRef[?]) {
    def toURI: URI = new URI(Serialization.serializedActorPath(actorRef.toClassic))
  }
}
