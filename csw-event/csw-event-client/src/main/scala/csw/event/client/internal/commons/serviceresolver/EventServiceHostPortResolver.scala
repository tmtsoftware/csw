/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.commons.serviceresolver

import java.net.URI

import scala.concurrent.Future

/**
 * Provides the connection information of `Event Service` by using the provided host and port.
 */
private[event] class EventServiceHostPortResolver(host: String, port: Int) extends EventServiceResolver {
  override def uri(): Future[URI] = Future.successful(new URI(s"tcp://$host:$port"))
}
