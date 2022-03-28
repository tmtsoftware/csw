/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.handlers

import csw.command.api.messages.CommandServiceRequest

object TestHelper {
  implicit class Narrower(x: CommandServiceRequest) {
    def narrow: CommandServiceRequest = x
  }

}
