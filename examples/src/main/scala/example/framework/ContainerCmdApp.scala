/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.framework

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.CSW

//#container-app
object ContainerCmdApp {
  def main(args: Array[String]): Unit = {
    ContainerCmd.start("ContainerCmdApp", CSW, args)
  }
}
//#container-app
