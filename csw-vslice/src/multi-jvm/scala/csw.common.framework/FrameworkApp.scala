package csw.common.framework

import com.typesafe.config.{Config, ConfigFactory}
import csw.common.framework.internal.wiring.{Container, FrameworkWiring}

object FrameworkApp extends App {
  private val wiring         = new FrameworkWiring
  private val config: Config = ConfigFactory.load("wfs_container.conf")

  Container.spawn(config, wiring)
}
