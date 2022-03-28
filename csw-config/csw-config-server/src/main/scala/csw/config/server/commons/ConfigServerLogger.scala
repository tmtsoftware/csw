/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.server.commons

import csw.logging.client.scaladsl.LoggerFactory

/**
 * All the logs generated from config service will have a fixed prefix, which is picked from `ConfigServiceConnection`.
 * The componentName helps in production to filter out logs from a particular component and this case, it helps to filter out logs
 * generated from config service.
 */
object ConfigServerLogger extends LoggerFactory(ConfigServiceConnection.value.prefix)
