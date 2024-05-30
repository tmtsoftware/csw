/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.extensions

import java.net.URI

import org.apache.pekko.actor.typed.{ActorRef, ActorRefResolver, ActorSystem}

object URIExtension {
  implicit class RichURI(val uri: URI) {
    def toActorRef(implicit system: ActorSystem[?]): ActorRef[Nothing] = ActorRefResolver(system).resolveActorRef(uri.toString)
  }
}
