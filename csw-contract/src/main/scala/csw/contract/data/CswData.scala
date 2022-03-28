/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract.data

import csw.contract.data.command.CommandContract
import csw.contract.data.location.LocationContract
import csw.contract.data.config.ConfigContract
import csw.contract.generator.Services

object CswData {
  val services: Services = Services(
    Map(
      "location-service" -> LocationContract.service,
      "command-service"  -> CommandContract.service,
      "config-service"   -> ConfigContract.service
    )
  )
}
