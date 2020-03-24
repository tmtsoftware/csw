package example.framework

import csw.framework.deploy.hostconfig.HostConfig
import csw.prefix.models.Subsystem.CSW

//#host-config-app
object HostConfigApp extends App {

  HostConfig.start("HostConfigApp", CSW, args)

}
//#host-config-app
