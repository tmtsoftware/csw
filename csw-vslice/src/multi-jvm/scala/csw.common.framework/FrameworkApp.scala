package csw.common.framework

import com.typesafe.config.ConfigFactory
import csw.common.framework.scaladsl.Component

object FrameworkApp extends App {

  Component.createContainer(ConfigFactory.load("wfs_container.conf"))

}
