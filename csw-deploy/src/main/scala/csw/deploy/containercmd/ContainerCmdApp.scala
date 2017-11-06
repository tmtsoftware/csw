package csw.deploy.containercmd

import csw.apps.containercmd.ContainerCmd

object ContainerCmdApp extends App {

  ContainerCmd.start("ContainerCmdApp", args)

}
