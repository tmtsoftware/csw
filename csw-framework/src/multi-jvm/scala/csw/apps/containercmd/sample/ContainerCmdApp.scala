package csw.apps.containercmd.sample

import csw.apps.containercmd.ContainerCmd

// DEOPSCSW-171: Starting component from command line
object ContainerCmdApp extends App {
  ContainerCmd.start(args)
}
