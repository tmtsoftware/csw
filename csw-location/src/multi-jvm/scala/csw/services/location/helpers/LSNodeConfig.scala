package csw.services.location.helpers

import akka.actor.ActorSystem
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.Config
import csw.services.location.internal.Settings

class LSNodeConfig extends MultiNodeConfig {
  private val settings = Settings()

  def seedRole(name: String): RoleName = addRole(name)(settings.asSeed)
  def memberRole(name: String): RoleName = addRole(name)(settings.joinLocalSeed)

  def addRole(name: String)(settings: Settings): RoleName = {
    val node = role(name)
    nodeConfig(node)(settings.config)
    node
  }

  def makeSystem(config: Config): ActorSystem = ActorSystem(settings.clusterName, config)
}

object LSNodeConfig {

  class TwoNodes extends LSNodeConfig {
    val node1: RoleName = seedRole("node-1")
    val node2: RoleName = memberRole("node-2")
  }

  class ThreeNodes extends TwoNodes {
    val node3: RoleName = memberRole("node-3")
  }

}
