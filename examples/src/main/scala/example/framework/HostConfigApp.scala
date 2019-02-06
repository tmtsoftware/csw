package example.framework

import csw.framework.deploy.hostconfig.HostConfig

//#host-config-app
object HostConfigApp extends App {

  HostConfig.start("Host-Config-App", args)

}
//#host-config-app
