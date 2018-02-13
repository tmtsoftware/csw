package csw.framework

import csw.apps.containercmd.ContainerCmd

//#container-app
object ContainerCmdApp extends App {

  ContainerCmd.start("Container-Cmd-App", args)

}
//#container-app
