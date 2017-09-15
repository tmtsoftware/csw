package csw.apps.containercmd.sample

import csw.apps.containercmd.ContainerCmd

// DEOPSCSW-171: Starting component from command line
object ContainerCmdApp extends App {
  // name which will appear in log statements as `@componentName`
  val componentName = "Sample-App"
  ContainerCmd.start(componentName, args)
}
