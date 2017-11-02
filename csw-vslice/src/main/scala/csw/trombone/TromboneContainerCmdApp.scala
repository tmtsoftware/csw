package csw.trombone

import csw.apps.deployment.containercmd.ContainerCmd

object TromboneContainerCmdApp extends App {
  ContainerCmd.start("vslice", args)
}
