package csw.services.location.helpers

import akka.actor.ActorSystem
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.Config
import csw.services.location.internal.Settings

class LSThreeNodeConfig extends MultiNodeConfig {

  val node1: RoleName = role("node-1")
  val node2: RoleName = role("node-2")
  val node3: RoleName = role("node-3")

  private val settings = Settings()

  nodeConfig(node1)(settings.asSeed.config)
  nodeConfig(node2)(settings.joinLocalSeed.config)
  nodeConfig(node3)(settings.joinLocalSeed.config)

  def makeSystem(config: Config): ActorSystem = ActorSystem(settings.name, config)
}
