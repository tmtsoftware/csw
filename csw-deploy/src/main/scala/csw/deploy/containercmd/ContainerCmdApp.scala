package csw.deploy.containercmd

import csw.apps.deployment.containercmd.ContainerCmd

object ContainerCmdApp extends App {

  ContainerCmd.start("ContainerCmdApp", args)

}
