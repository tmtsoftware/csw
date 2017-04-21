package csw.services.config.helpers

import akka.actor.ActorSystem
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.Config
import csw.services.location.commons.ClusterSettings

class NMembersAndSeed(n: Int) extends MultiNodeConfig {

  private val settings = ClusterSettings()

  def makeSystem(config: Config): ActorSystem = ActorSystem(settings.clusterName, config)

  val seed: RoleName = addRole("seed")(settings.onPort(3552))

  val members: Vector[RoleName] = (1 to n).toVector.map { x =>
    addRole(s"member-$x")(settings.joinLocal(3552))
  }

  private def addRole(name: String)(settings: ClusterSettings): RoleName = {
    val node = role(name)
    nodeConfig(node)(settings.config)
    node
  }
}

class OneClientAndServer extends NMembersAndSeed(1) {
  val server: RoleName = seed
  val Vector(client)   = members
}
