package csw.deploy.containercmd

import csw.apps.containercmd.ContainerCmd

// $COVERAGE-OFF$
object ContainerCmdApp extends App {

  ContainerCmd.start("ContainerCmdApp", args)

}
// $COVERAGE-ON$
