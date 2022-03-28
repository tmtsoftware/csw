/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.commons

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

/**
 * All the logs generated from location service will have a fixed prefix, which is the value of [[Constants.LocationService]].
 * The prefix helps in production to filter out logs from a particular component and this case, it helps to filter out logs
 * generated from location service.
 */
private[location] object LocationServiceLogger extends LoggerFactory(Prefix(CSW, Constants.LocationService))
