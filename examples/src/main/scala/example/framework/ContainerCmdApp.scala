package example.framework

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.CSW

//#container-app
object ContainerCmdApp extends App {

  ContainerCmd.start("Container-Cmd-App", CSW, args)

}
//#container-app
