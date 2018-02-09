package csw.trombone

import csw.apps.containercmd.ContainerCmd

//#container-app
object TromboneContainerCmdApp extends App {
  System.setProperty("clusterSeeds", "10.131.125.210:3552")
  System.setProperty("clusterPort", "3552")
  val args1 = Array(
    "--standalone",
    "--local",
    "/Users/pritamkadam/TMT/csw-prod/csw-vslice/src/main/resources/hcdStandalone.conf"
  )
  ContainerCmd.start("Trombone-Container-Cmd-App", args1)
}
//#container-app
