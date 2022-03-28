/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.framework

import csw.framework.deploy.hostconfig.HostConfig
import csw.prefix.models.Subsystem.CSW

//#host-config-app
object HostConfigApp extends App {

  HostConfig.start("HostConfigApp", CSW, args)

}
//#host-config-app
