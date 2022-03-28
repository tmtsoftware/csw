/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.messages

import csw.location.api.models.Connection

sealed trait LocationStreamRequest

object LocationStreamRequest {
  case class Track(connection: Connection) extends LocationStreamRequest
}
