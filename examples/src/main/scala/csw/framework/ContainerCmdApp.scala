package csw.framework

import csw.framework.deploy.containercmd.ContainerCmd

//#container-app
object ContainerCmdApp extends App {

  ContainerCmd.start("Container-Cmd-App", args)

}
//#container-app
