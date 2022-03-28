/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.wrapper

import csw.location.server.internal.ServerWiring

// workaround to access server wiring which is private to csw in example app
class LocationServerWiring(val wiring: ServerWiring = new ServerWiring(enableAuth = false))
