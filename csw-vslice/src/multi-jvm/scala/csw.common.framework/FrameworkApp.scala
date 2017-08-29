package csw.common.framework

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.common.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.services.location.commons.ClusterSettings

object FrameworkApp extends App {
  private val clusterSettings: ClusterSettings = ClusterSettings().withManagementPort(5555)
  private val system: ActorSystem              = clusterSettings.system
  private val wiring                           = FrameworkWiring.make(system)
  private val config: Config                   = ConfigFactory.load("eaton_hcd_standalone.conf")

  Standalone.spawn(config, wiring)
}
