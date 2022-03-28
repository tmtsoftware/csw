/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.commons.serviceresolver

import java.net.URI

import scala.concurrent.Future

/**
 * Base trait for implementations providing connection information for Event Service
 */
private[event] trait EventServiceResolver {
  def uri(): Future[URI]
}
