package csw.trombone

import csw.apps.containercmd.ContainerCmd

object TromboneContainerCmdApp extends App {
  ContainerCmd.start("vslice", args)
}
