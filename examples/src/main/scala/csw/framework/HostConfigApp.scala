package csw.framework

import csw.apps.hostconfig.HostConfig

//#host-config-app
object HostConfigApp extends App {

  HostConfig.start("Host-Config-App", args)

}
//#host-config-app
