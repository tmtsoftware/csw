package csw.common.framework

import com.typesafe.config.ConfigFactory
import csw.common.framework.internal.wiring.{Container, FrameworkWiring}

object FrameworkApp extends App {

  Container.spawn(ConfigFactory.load("wfs_container.conf"), FrameworkWiring.make())

}
