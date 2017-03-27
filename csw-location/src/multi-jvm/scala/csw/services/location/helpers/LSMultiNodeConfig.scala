package csw.services.location.helpers

import akka.actor.ActorSystem
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.Config
import csw.services.location.internal.Settings

class LSMultiNodeConfig extends MultiNodeConfig {

  val node1: RoleName = role("node-1")
  val node2: RoleName = role("node-2")

  private val settings = Settings()

  nodeConfig(node1)(settings.withPort(2552).config)
  nodeConfig(node2)(settings.withPort(2553).config)

  def makeSystem(config: Config): ActorSystem = ActorSystem(settings.name, config)
}
