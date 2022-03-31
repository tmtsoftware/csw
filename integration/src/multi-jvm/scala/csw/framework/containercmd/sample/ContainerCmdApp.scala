/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.containercmd.sample

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.CSW

// DEOPSCSW-171: Starting component from command line
object ContainerCmdApp extends App {
  // name which will appear in log statements as `@componentName`
  val componentName = "Sample-App"
  ContainerCmd.start(componentName, CSW, args)
}
