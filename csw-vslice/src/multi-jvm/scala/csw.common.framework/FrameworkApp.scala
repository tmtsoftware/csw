package csw.common.framework

import com.typesafe.config.ConfigFactory

import csw.common.framework.internal.container
import csw.common.framework.internal.wiring.Container

object FrameworkApp extends App {

  Container.spawn(ConfigFactory.load("wfs_container.conf"))

}
